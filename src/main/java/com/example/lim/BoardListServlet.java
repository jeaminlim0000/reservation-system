package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/BoardListServlet")
public class BoardListServlet extends HttpServlet {

    // DB 연결 정보
    private static final String DB_URL = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD"; // 실제 비밀번호

    // 한 페이지에 보여줄 게시글 수
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 인코딩 설정 (한글 페이지 파라미터 대비)
        request.setCharacterEncoding("UTF-8");

        // 1) 페이지 번호 파라미터 (예: ?page=2)
        String pageParam = request.getParameter("page");
        int currentPage = (pageParam != null) ? Integer.parseInt(pageParam) : 1;
        if (currentPage < 1) currentPage = 1;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 2) JDBC 드라이버 로드 & DB 연결
            Class.forName("com.mysql.jdbc.Driver"); // 또는 com.mysql.cj.jdbc.Driver
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 3) 전체 게시글 수 조회
            String countSql = "SELECT COUNT(*) FROM board_post";
            // board_post 테이블명은 실제 사용 중인 테이블에 맞춰 수정
            pstmt = conn.prepareStatement(countSql);
            rs = pstmt.executeQuery();

            int totalCount = 0;
            if (rs.next()) {
                totalCount = rs.getInt(1);
            }
            rs.close();
            pstmt.close();

            // 총 페이지 수 계산
            int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            if (totalPage < 1) totalPage = 1;
            if (currentPage > totalPage) currentPage = totalPage;

            // 4) 현재 페이지에 해당하는 게시글 목록 가져오기
            int startIndex = (currentPage - 1) * PAGE_SIZE;
            String listSql = "SELECT * FROM board_post ORDER BY id DESC LIMIT ?, ?";
            pstmt = conn.prepareStatement(listSql);
            pstmt.setInt(1, startIndex);
            pstmt.setInt(2, PAGE_SIZE);
            rs = pstmt.executeQuery();

            // 게시글 정보를 담을 리스트
            List<Post> postList = new ArrayList<Post>();
            while (rs.next()) {
                Post post = new Post();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setWriter(rs.getString("writer"));
                post.setCreatedAt(rs.getTimestamp("created_at"));
                postList.add(post);
            }

            // 5) JSP로 전달할 데이터 세팅
            request.setAttribute("postList", postList);
            request.setAttribute("currentPage", currentPage);
            request.setAttribute("totalPage", totalPage);
            request.setAttribute("totalCount", totalCount);

            // board.jsp로 forward
            request.getRequestDispatcher("/board.jsp").forward(request, response);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.getWriter().println("DB 에러: " + e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("DB 에러: " + e.getMessage());
        } finally {
            // 자원 정리
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
