package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class AdminToolListServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    // 목록 조회는 누구나 보이도록 허용(페이지 표시 목적)
    // 필요 시 write/modify API만 관리자 제한을 두면 된다.
    private boolean canView(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        String u = (s != null) ? (String) s.getAttribute("loggedInUser") : null;
        return true;                  //  조회는 항상 허용
        // return "admin".equals(u);  // 관리자만 보이게 하려면 이 줄로 교체
    }

    static class Row {
        int id;
        String name;
        String category;
        String department;
        String location;
        String addedAt;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!canView(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        // 응답 헤더
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        List<Row> list = new ArrayList<Row>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.jdbc.Driver"); // MySQL 옛날버전 .cj  없는거
            conn = DriverManager.getConnection(URL, USER, PW);

            ps = conn.prepareStatement(
                    "SELECT id, name, category, department, location, added_at " +
                            "FROM tool ORDER BY id DESC"
            );
            rs = ps.executeQuery();

            while (rs.next()) {
                Row r = new Row();
                r.id         = rs.getInt("id");
                r.name       = rs.getString("name");
                r.category   = rs.getString("category");
                r.department = rs.getString("department");
                r.location   = rs.getString("location");
                Timestamp ts = rs.getTimestamp("added_at");
                r.addedAt    = (ts != null ? ts.toString() : "");
                list.add(r);
            }

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(list));
            out.flush();

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            try { if (rs   != null) rs.close(); } catch (SQLException ignore) {}
            try { if (ps   != null) ps.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }
}
