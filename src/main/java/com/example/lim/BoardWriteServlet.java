package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/BoardWriteServlet")
public class BoardWriteServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";  // 실제 비밀번호

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 인코딩 설정
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 파라미터 받기
        String title = request.getParameter("title");
        String writer = request.getParameter("writer");
        String content = request.getParameter("content");

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // INSERT 쿼리
            String sql = "INSERT INTO board_post (title, content, writer, created_at) " +
                    "VALUES (?, ?, ?, NOW())";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setString(3, writer);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                // 글 작성 성공 → 글 목록으로 이동
                response.sendRedirect("BoardListServlet");
            } else {
                // 실패 시 에러 메시지
                response.getWriter().println("글쓰기 실패! 다시 시도하세요.");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.getWriter().println("드라이버 로드 실패");
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("DB 에러: " + e.getMessage());
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
