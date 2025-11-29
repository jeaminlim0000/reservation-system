package com.example.lim;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.sql.*;

public class ScheduleServlet extends HttpServlet {
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER     = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) 요청 파라미터
        String campus   = req.getParameter("campus");
        String building = req.getParameter("building");
        String floorStr = req.getParameter("floor");
        String room     = req.getParameter("room");
        String dateStr  = req.getParameter("date");

        int floor = 1;
        try {
            floor = Integer.parseInt(floorStr);
        } catch (NumberFormatException ignore) {}

        resp.setContentType("application/json; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        try {

            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 2) 날짜까지 포함한 쿼리 작성
            String sql =
                    "SELECT timeslot " +
                            "  FROM reservation " +
                            " WHERE campus=? " +
                            "   AND building=? " +
                            "   AND floor=? " +
                            "   AND room=? " +
                            "   AND reserved_date=?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt   (3, floor);
            ps.setString(4, room);
            // ▶ Date 파라미터도 바인딩
            ps.setDate  (5, java.sql.Date.valueOf(dateStr));

            rs = ps.executeQuery();

            // 3) JSON 배열로 내보내기
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("\"").append(rs.getString("timeslot")).append("\"");
                first = false;
            }
            sb.append("]");

            out.print(sb.toString());

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            throw new ServletException("DB 조회 실패", e);
        } finally {
            // 4) 리소스 해제
            if (rs   != null) try { rs.close();   } catch (SQLException ign) {}
            if (ps   != null) try { ps.close();   } catch (SQLException ign) {}
            if (conn != null) try { conn.close(); } catch (SQLException ign) {}
        }
    }
}
