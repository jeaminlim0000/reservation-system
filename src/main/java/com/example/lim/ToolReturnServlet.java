package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ToolReturnServlet extends HttpServlet {
    private static final String URL  = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) 인코딩 및 응답 타입 설정
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        // 2) 로그인 사용자 확인
        HttpSession session = req.getSession(false);
        String user = (session != null)
                ? (String) session.getAttribute("loggedInUser")
                : null;
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        // 3) 요청 파라미터 검증
        String idParam   = req.getParameter("id");
        String dateParam = req.getParameter("date");
        String slotParam = req.getParameter("timeslot");
        if (idParam == null || dateParam == null || slotParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id, date, timeslot 파라미터가 필요합니다.");
            return;
        }

        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            // 4) JDBC 드라이버 로드 및 연결
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            // 5) DELETE 쿼리 실행
            String sql = "DELETE FROM tool_reservation " +
                    "WHERE user_id=? AND tool_id=? AND reserved_date=? AND timeslot=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            ps.setInt   (2, Integer.parseInt(idParam));
            ps.setDate  (3, java.sql.Date.valueOf(dateParam));
            ps.setString(4, slotParam);

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                resp.getWriter().print("OK");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "대여 내역이 없습니다.");
            }

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            // 6) 자원 해제
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
