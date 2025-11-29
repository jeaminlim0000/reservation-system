package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class RoomMetaServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String mode     = n(req.getParameter("mode"));
        String campus   = n(req.getParameter("campus"));
        String building = n(req.getParameter("building"));
        String floorStr = n(req.getParameter("floor"));

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PrintWriter out = resp.getWriter();

        List<Object> result = new ArrayList<Object>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            if ("campuses".equals(mode)) {
                ps = conn.prepareStatement("SELECT DISTINCT campus FROM room ORDER BY campus");
            } else if ("buildings".equals(mode)) {
                ps = conn.prepareStatement(
                        "SELECT DISTINCT building FROM room WHERE campus=? ORDER BY building");
                ps.setString(1, campus);
            } else if ("floors".equals(mode)) {
                ps = conn.prepareStatement(
                        "SELECT DISTINCT floor FROM room WHERE campus=? AND building=? ORDER BY floor");
                ps.setString(1, campus);
                ps.setString(2, building);
            } else if ("rooms".equals(mode)) {
                int floor = 0;
                try { floor = Integer.parseInt(floorStr); } catch (Exception ignore) {}
                ps = conn.prepareStatement(
                        "SELECT room_no FROM room WHERE campus=? AND building=? AND floor=? ORDER BY room_no");
                ps.setString(1, campus);
                ps.setString(2, building);
                ps.setInt(3, floor);
            } else {
                // 잘못된 요청
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("[]");
                return;
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                if ("floors".equals(mode)) {
                    result.add(Integer.valueOf(rs.getInt(1)));
                } else {
                    result.add(rs.getString(1));
                }
            }

            out.print(new Gson().toJson(result));

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
        }
    }

    private static String n(String s) { return s == null ? "" : s; }
}
