package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/BoardDetailServlet")
public class BoardDetailServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD"; // 실제 비밀번호

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // 1) 파라미터 id 받기 (예: ?id=5)
        String idParam = request.getParameter("id");
        if (idParam == null) {
            // id가 없으면 목록으로 되돌리거나 에러 처리
            response.sendRedirect("BoardListServlet");
            return;
        }

        int postId = Integer.parseInt(idParam);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Post post = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 2) SELECT 쿼리: 해당 id의 게시글만 조회
            String sql = "SELECT * FROM board_post WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, postId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                post = new Post();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setWriter(rs.getString("writer"));
                post.setCreatedAt(rs.getTimestamp("created_at"));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.getWriter().println("드라이버 로드 실패");
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("DB 에러: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        // 3) 게시글 정보가 있으면 JSP로 forward, 없으면 목록으로
        if (post != null) {
            request.setAttribute("post", post);
            request.getRequestDispatcher("/boardDetail.jsp").forward(request, response);
        } else {
            response.sendRedirect("BoardListServlet");
        }
    }
}
