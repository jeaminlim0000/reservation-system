package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class ToolScheduleServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Connection   conn = null;
        PreparedStatement ps = null;
        ResultSet    rs   = null;
        PrintWriter  out  = resp.getWriter();

        resp.setContentType("application/json; charset=UTF-8");

        String idParam   = req.getParameter("id");
        String dateParam = req.getParameter("date");
        if (idParam == null || dateParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id와 date 파라미터가 필요합니다.");
            return;
        }

        try {
            // 1) JDBC 드라이버 로드
            Class.forName("com.mysql.jdbc.Driver");

            // 2) 커넥션 생성
            conn = DriverManager.getConnection(URL, USER, PW);

            // 3) 쿼리 준비 및 실행
            String sql = "SELECT timeslot FROM tool_reservation WHERE tool_id = ? AND reserved_date = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(idParam));
            ps.setDate(2, java.sql.Date.valueOf(dateParam));
            rs = ps.executeQuery();

            // 4) 결과 수집
            List<String> times = new ArrayList<String>();
            while (rs.next()) {
                times.add(rs.getString("timeslot"));
            }

            // 5) JSON 출력
            String json = new Gson().toJson(times);
            out.print(json);

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());

        } finally {
            // 6) 리소스 해제
            if (rs != null) {
                try { rs.close(); } catch (SQLException ignored) {}
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
