package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class ToolMyLoansServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        PrintWriter out = resp.getWriter();

        // 로그인 사용자 확인
        HttpSession session = req.getSession(false);
        String user = (session != null) ? (String) session.getAttribute("loggedInUser") : null;
        if (user == null || user.length() == 0) {
            out.print("[]");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            // 오늘 포함, 미래 예약만 반환
            String sql =
                    "SELECT tr.tool_id, t.name, tr.timeslot, tr.reserved_date " +
                            "  FROM tool_reservation tr " +
                            "  JOIN tool t ON t.id = tr.tool_id " +
                            " WHERE tr.user_id = ? " +
                            "   AND tr.reserved_date >= CURDATE() " +   // ← 핵심 필터
                            " ORDER BY tr.reserved_date ASC, tr.timeslot ASC";

            ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            rs = ps.executeQuery();

            List<Map<String,Object>> loans = new ArrayList<Map<String,Object>>();
            while (rs.next()) {
                Map<String,Object> m = new HashMap<String,Object>();
                java.sql.Date d = rs.getDate("reserved_date");
                m.put("date", (d != null) ? d.toString() : "");
                m.put("name", rs.getString("name"));
                m.put("timeslot", rs.getString("timeslot"));
                m.put("id", rs.getInt("tool_id"));
                loans.add(m);
            }

            out.print(new Gson().toJson(loans));

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            if (rs != null)  try { rs.close(); } catch (SQLException ignore) {}
            if (ps != null)  try { ps.close(); } catch (SQLException ignore) {}
            if (conn != null)try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }
}
