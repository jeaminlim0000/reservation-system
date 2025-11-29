package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

// /api/history
public class HistoryServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Row {
        String date;      // yyyy-MM-dd
        String type;      // "교실" | "도구"
        String item;      // "Engineering 301호" | "노트북01"
        String start;     // "09:00"
        String end;       // "10:00" (교실 +1h, 도구 +2h)
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        HttpSession session = req.getSession(false);
        String user = (session != null) ? (String) session.getAttribute("loggedInUser") : null;
        if (user == null || user.length() == 0) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        Connection conn = null;
        PreparedStatement ps1 = null, ps2 = null;
        ResultSet rs1 = null, rs2 = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            List<Row> rows = new ArrayList<Row>();

            // 1) 교실(과거)
            String sql1 =
                    "SELECT reserved_date, timeslot, building, room " +
                            "  FROM reservation " +
                            " WHERE reserved_by=? AND reserved_date < CURDATE() " +
                            " ORDER BY reserved_date DESC, timeslot";
            ps1 = conn.prepareStatement(sql1);
            ps1.setString(1, user);
            rs1 = ps1.executeQuery();
            while (rs1.next()) {
                Row r = new Row();
                r.date  = rs1.getDate("reserved_date").toString();
                r.type  = "교실";
                r.item  = rs1.getString("building") + " " + rs1.getString("room") + "호";
                r.start = rs1.getString("timeslot");
                r.end   = addHours(r.start, 1); // 교실: 1시간
                rows.add(r);
            }

            // 2) 도구(과거)
            String sql2 =
                    "SELECT tr.reserved_date, tr.timeslot, t.name " +
                            "  FROM tool_reservation tr JOIN tool t ON tr.tool_id=t.id " +
                            " WHERE tr.user_id=? AND tr.reserved_date < CURDATE() " +
                            " ORDER BY tr.reserved_date DESC, tr.timeslot";
            ps2 = conn.prepareStatement(sql2);
            ps2.setString(1, user);
            rs2 = ps2.executeQuery();
            while (rs2.next()) {
                Row r = new Row();
                r.date  = rs2.getDate("reserved_date").toString();
                r.type  = "도구";
                r.item  = rs2.getString("name");
                r.start = rs2.getString("timeslot");
                r.end   = addHours(r.start, 2); // 도구: 2시간(슬롯 간격 기준)
                rows.add(r);
            }

            // 3) 최종 정렬(날짜 DESC, 시작시간 ASC)
            Collections.sort(rows, new Comparator<Row>() {
                public int compare(Row a, Row b) {
                    int d = b.date.compareTo(a.date);
                    if (d != 0) return d;
                    return a.start.compareTo(b.start);
                }
            });

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(rows));

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            if (rs1 != null) try { rs1.close(); } catch (SQLException ignored) {}
            if (rs2 != null) try { rs2.close(); } catch (SQLException ignored) {}
            if (ps1 != null) try { ps1.close(); } catch (SQLException ignored) {}
            if (ps2 != null) try { ps2.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // "HH:mm" + hours → "HH:mm"
    private static String addHours(String hhmm, int hours) {
        try {
            int h = Integer.parseInt(hhmm.substring(0, 2));
            int m = Integer.parseInt(hhmm.substring(3, 5));
            h = (h + hours) % 24;
            String hs = (h < 10 ? "0" : "") + h;
            String ms = (m < 10 ? "0" : "") + m;
            return hs + ":" + ms;
        } catch (Exception ignore) {
            return hhmm;
        }
    }
}
