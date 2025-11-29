package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class DeleteReservationServlet extends HttpServlet {
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DB_USER     = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String campus   = req.getParameter("campus");
        String building = req.getParameter("building");
        String floorStr = req.getParameter("floor");
        String room     = req.getParameter("room");
        String timeslot = req.getParameter("timeslot");
        String dateStr  = req.getParameter("date");   // yyyy-MM-dd

        int floor = 1;
        try {
            floor = Integer.parseInt(floorStr);
        } catch (NumberFormatException ignore) {}

        HttpSession session = req.getSession(false);
        String user = (session != null) ? (String) session.getAttribute("loggedInUser") : "";

        Connection conn = null;
        PreparedStatement ps = null;
        PrintWriter out = resp.getWriter();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String sql =
                    "DELETE FROM reservation " +
                            " WHERE campus=? AND building=? AND floor=? AND room=? " +
                            "   AND timeslot=? AND reserved_date=? AND reserved_by=?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, room);
            ps.setString(5, timeslot);
            ps.setDate(6, java.sql.Date.valueOf(dateStr));
            ps.setString(7, user);

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.print("OK");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "삭제할 예약이 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "예약 삭제 중 오류 발생");
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
        }
    }
}
