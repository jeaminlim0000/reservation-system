package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

/**
 * /admin/tool/meta
 * tool 테이블에서 DISTINCT 값들(카테고리/전공/위치)을 뽑아 JSON으로 반환
 * { "categories":[..], "departments":[..], "locations":[..] }
 */
public class AdminToolMetaServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    private boolean isAdmin(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        String u = (s!=null) ? (String)s.getAttribute("loggedInUser") : null;
        // 필요하다면 권한 체크를 완화해도 됨
        return u != null && (u.equals("admin") || u.length() > 0);
    }

    static class Meta {
        List<String> categories = new ArrayList<String>();
        List<String> departments = new ArrayList<String>();
        List<String> locations = new ArrayList<String>();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 관리자만 막고 싶다면 주석 해제
        // if(!isAdmin(req)){ resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Meta meta = new Meta();

        try {
            Class.forName("com.mysql.jdbc.Driver"); // MySQL 5.x
            conn = DriverManager.getConnection(URL, USER, PW);

            // 1) 카테고리
            ps = conn.prepareStatement("SELECT DISTINCT category FROM tool WHERE category IS NOT NULL AND category<>'' ORDER BY category");
            rs = ps.executeQuery();
            while (rs.next()) meta.categories.add(rs.getString(1));
            rs.close(); ps.close();

            // 2) 전공(학부)
            ps = conn.prepareStatement("SELECT DISTINCT department FROM tool WHERE department IS NOT NULL AND department<>'' ORDER BY department");
            rs = ps.executeQuery();
            while (rs.next()) meta.departments.add(rs.getString(1));
            rs.close(); ps.close();

            // 3) 위치
            ps = conn.prepareStatement("SELECT DISTINCT location FROM tool WHERE location IS NOT NULL AND location<>'' ORDER BY location");
            rs = ps.executeQuery();
            while (rs.next()) meta.locations.add(rs.getString(1));
            rs.close(); ps.close();

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(meta));

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignore) {}
            try { if (ps != null) ps.close(); } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }
}
