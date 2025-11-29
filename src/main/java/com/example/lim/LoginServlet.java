package com.example.lim;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.sql.*;

public class LoginServlet extends HttpServlet {
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER     = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1) 한글 및 응답 인코딩
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        // 2) 폼 파라미터
        String username   = request.getParameter("username");      // <input name="username">
        String password   = request.getParameter("password");      // <input name="password">
        String rememberMe = request.getParameter("rememberMe");    // 체크박스 선택 여부

        Connection conn       = null;
        PreparedStatement pstmt = null;
        ResultSet rs          = null;

        try {
            // 3) JDBC 드라이버 로드 (Java 1.6 스타일)
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 4) 인증 쿼리 (id + pwd)
            pstmt = conn.prepareStatement(
                    "SELECT * FROM user_info WHERE id = ? AND pwd = ?"
            );
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // 로그인 성공: 세션에 사용자 정보 저장
                HttpSession session = request.getSession();
                session.setAttribute("loggedInUser", username);

                // 아이디 기억하기: 선택사항으로 처리
                if (rememberMe != null) {
                    Cookie cookie = new Cookie("username", username);
                    cookie.setMaxAge(7 * 24 * 60 * 60);  // 7일
                    cookie.setPath(request.getContextPath());
                    response.addCookie(cookie);
                }

                // 성공 시 board.jsp로 이동
                response.sendRedirect(request.getContextPath() + "/main.jsp");
            } else {
                // 로그인 실패 시 index.jsp?error=1
                response.sendRedirect(request.getContextPath() + "/index.jsp?error=1");
            }

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            throw new ServletException("DB 에러", e);
        } finally {
            // 5) 리소스 해제 (Java 1.6 스타일)
            if (rs    != null) try { rs.close();    } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            if (conn  != null) try { conn.close();  } catch (SQLException ignored) {}
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // GET 요청은 로그인 폼으로 리다이렉트
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }
}
