package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class ReservationServlet extends HttpServlet {
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String action = req.getParameter("action");   // "list" | null(insert)

        // 로그인 사용자
        HttpSession session = req.getSession(false);
        String user = (session != null) ? (String) session.getAttribute("loggedInUser") : "";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);


            // 내 예약 목록 (오늘 포함, 미래만)
            if ("list".equals(action)) {
                resp.setContentType("application/json; charset=UTF-8");

                String sql =
                        "SELECT campus, building, floor, room, timeslot, reserved_date " +
                                "  FROM reservation " +
                                " WHERE reserved_by = ? " +
                                "   AND reserved_date >= CURDATE() " +     // ★ 핵심: 과거 숨김
                                " ORDER BY reserved_date ASC, timeslot ASC";

                ps = conn.prepareStatement(sql);
                ps.setString(1, user);
                rs = ps.executeQuery();

                StringBuilder sb = new StringBuilder(256);
                sb.append("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(',');
                    sb.append('{')
                            .append("\"campus\":\"").append(rs.getString("campus")).append("\",")
                            .append("\"building\":\"").append(rs.getString("building")).append("\",")
                            .append("\"floor\":").append(rs.getInt("floor")).append(',')
                            .append("\"room\":\"").append(rs.getString("room")).append("\",")
                            .append("\"timeslot\":\"").append(rs.getString("timeslot")).append("\",")
                            .append("\"date\":\"").append(rs.getDate("reserved_date")).append("\"")
                            .append('}');
                    first = false;
                }
                sb.append("]");
                resp.getWriter().print(sb.toString());
                return;
            }


            // 신규 예약 INSERT
            String campus   = req.getParameter("campus");
            String building = req.getParameter("building");
            int    floor    = Integer.parseInt(req.getParameter("floor"));
            String room     = req.getParameter("room");
            String timeslot = req.getParameter("timeslot");
            String dateStr  = req.getParameter("date"); // yyyy-MM-dd

            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();

            // 중복 방지(동시성 안전용 2차 체크)
            String dupCheck =
                    "SELECT COUNT(*) FROM reservation " +
                            " WHERE campus=? AND building=? AND floor=? AND room=? AND timeslot=? AND reserved_date=?";
            ps = conn.prepareStatement(dupCheck);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, room);
            ps.setString(5, timeslot);
            ps.setDate(6, java.sql.Date.valueOf(dateStr));
            rs = ps.executeQuery();
            int cnt = 0;
            if (rs.next()) cnt = rs.getInt(1);
            rs.close(); ps.close();

            if (cnt > 0) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("DUPLICATE");
                return;
            }

            String ins =
                    "INSERT INTO reservation " +
                            "(campus, building, floor, room, timeslot, reserved_by, reserved_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(ins);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, room);
            ps.setString(5, timeslot);
            ps.setString(6, user);
            ps.setDate(7, java.sql.Date.valueOf(dateStr));
            ps.executeUpdate();

            out.print("OK");

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류 발생: " + e.getMessage());
        } finally {
            try { if (rs   != null) rs.close();   } catch (SQLException ignore) {}
            try { if (ps   != null) ps.close();   } catch (SQLException ignore) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignore) {}
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }
}
