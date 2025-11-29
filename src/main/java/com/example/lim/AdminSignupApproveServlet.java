package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

@WebServlet("/admin/signup/approve")
public class AdminSignupApproveServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res { public String result; public String message;
        Res(String r, String m){ result=r; message=m; } }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        String u = (s != null) ? (String) s.getAttribute("loggedInUser") : null;
        return "admin".equals(u);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!isAdmin(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String idStr  = req.getParameter("id");           // req_id
        String action = req.getParameter("action");        // approve | reject
        PrintWriter out = resp.getWriter();

        int reqId;
        try {
            if (idStr==null || action==null) throw new IllegalArgumentException();
            reqId = Integer.parseInt(idStr);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id, action 필요 또는 형식 불일치");
            return;
        }

        Connection conn = null;
        PreparedStatement psSel=null, psChk=null, psIns=null, psUpd=null, psLog=null;
        ResultSet rs=null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);
            conn.setAutoCommit(false);

            // 1) 요청 잠금 + 현재 상태/데이터 조회
            String sqlSel =
                    "SELECT user_id, pwd, name, email, department, status " +
                            "  FROM user_signup_request WHERE req_id=? FOR UPDATE";
            psSel = conn.prepareStatement(sqlSel);
            psSel.setInt(1, reqId);
            rs = psSel.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                out.print(new Gson().toJson(new Res("ERROR","해당 요청을 찾을 수 없습니다.")));
                return;
            }
            String curStatus  = rs.getString("status");
            String userId     = rs.getString("user_id");
            String pwd        = rs.getString("pwd");
            String name       = rs.getString("name");
            String email      = rs.getString("email");
            String department = rs.getString("department");
            String decider    = "admin"; // 세션에서 꺼낸 관리자 id

            if (!"PENDING".equalsIgnoreCase(curStatus)) {
                conn.rollback();
                out.print(new Gson().toJson(new Res("ERROR","이미 처리된 요청입니다.")));
                return;
            }

            if ("approve".equalsIgnoreCase(action)) {
                // 2) 아이디 중복 검사
                String sqlChk = "SELECT 1 FROM user_info WHERE id = ?";
                psChk = conn.prepareStatement(sqlChk);
                psChk.setString(1, userId);
                ResultSet rsChk = psChk.executeQuery();
                if (rsChk.next()) {
                    rsChk.close();
                    conn.rollback();
                    out.print(new Gson().toJson(new Res("ERROR","이미 가입된 아이디입니다.")));
                    return;
                }
                rsChk.close();

                // 3) 계정 생성
                String sqlIns =
                        "INSERT INTO user_info (id, pwd, name, email, department, reg_date) " +
                                "VALUES (?, ?, ?, ?, ?, NOW())";
                psIns = conn.prepareStatement(sqlIns);
                psIns.setString(1, userId);
                psIns.setString(2, pwd);
                psIns.setString(3, name);
                psIns.setString(4, email);
                psIns.setString(5, department);
                psIns.executeUpdate();

                // 4) 요청 상태 업데이트
                String sqlUpd =
                        "UPDATE user_signup_request " +
                                "   SET status='APPROVED', decided_at=NOW(), decided_by=? " +
                                " WHERE req_id=?";
                psUpd = conn.prepareStatement(sqlUpd);
                psUpd.setString(1, decider);
                psUpd.setInt(2, reqId);
                psUpd.executeUpdate();

                // 5) 로그 적재
                String logSql =
                        "INSERT INTO user_signup_request_log " +
                                "(req_id, prev_status, new_status, changed_by, note) " +
                                "VALUES (?, ?, ?, ?, ?)";
                psLog = conn.prepareStatement(logSql);
                psLog.setInt(1, reqId);
                psLog.setString(2, curStatus);
                psLog.setString(3, "APPROVED");
                psLog.setString(4, decider);
                psLog.setString(5, null);
                psLog.executeUpdate();

                conn.commit();
                out.print(new Gson().toJson(new Res("OK","승인 완료")));

            } else if ("reject".equalsIgnoreCase(action)) {

                String sqlUpd =
                        "UPDATE user_signup_request " +
                                "   SET status='REJECTED', decided_at=NOW(), decided_by=? " +
                                " WHERE req_id=?";
                psUpd = conn.prepareStatement(sqlUpd);
                psUpd.setString(1, decider);
                psUpd.setInt(2, reqId);
                psUpd.executeUpdate();

                String logSql =
                        "INSERT INTO user_signup_request_log " +
                                "(req_id, prev_status, new_status, changed_by, note) " +
                                "VALUES (?, ?, ?, ?, ?)";
                psLog = conn.prepareStatement(logSql);
                psLog.setInt(1, reqId);
                psLog.setString(2, curStatus);
                psLog.setString(3, "REJECTED");
                psLog.setString(4, decider);
                psLog.setString(5, null);
                psLog.executeUpdate();

                conn.commit();
                out.print(new Gson().toJson(new Res("OK","거절 완료")));

            } else {
                conn.rollback();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "action 값은 approve 또는 reject 여야 합니다.");
            }

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            e.printStackTrace();
            out.print(new Gson().toJson(new Res("ERROR","DB 오류: " + e.getMessage())));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            out.print(new Gson().toJson(new Res("ERROR","드라이버 로드 실패")));
        } finally {
            try { if (rs!=null) rs.close(); } catch (SQLException ignore) {}
            try { if (psSel!=null) psSel.close(); } catch (SQLException ignore) {}
            try { if (psChk!=null) psChk.close(); } catch (SQLException ignore) {}
            try { if (psIns!=null) psIns.close(); } catch (SQLException ignore) {}
            try { if (psUpd!=null) psUpd.close(); } catch (SQLException ignore) {}
            try { if (psLog!=null) psLog.close(); } catch (SQLException ignore) {}
            try {
                if (conn!=null) { conn.setAutoCommit(true); conn.close(); }
            } catch (SQLException ignore) {}
        }
    }
}
