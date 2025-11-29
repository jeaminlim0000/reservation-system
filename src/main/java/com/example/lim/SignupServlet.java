package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    // 다른 서블릿들과 맞춰서 인코딩 옵션까지 붙이는 걸 추천
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1) 요청/응답 인코딩
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 2) 폼에서 넘어온 데이터
        String userId     = request.getParameter("userId");
        String password   = request.getParameter("password");
        String confirmPwd = request.getParameter("confirmPassword"); // 비밀번호 확인 칸 name
        String userName   = request.getParameter("userName");
        String email      = request.getParameter("email");
        String department = request.getParameter("department");

        // 2-1) admin 아이디 막기
        if ("admin".equalsIgnoreCase(userId)) {
            response.getWriter().println("이 아이디는 사용할 수 없습니다.");
            return;
        }

        // 2-2) 비밀번호 확인 체크 (JSP에서 confirmPassword 필드가 있을 때)
        if (confirmPwd != null && !password.equals(confirmPwd)) {
            response.getWriter().println("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // 3) JDBC 연결
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            /* 테이블 설명랑
             * 여기서 핵심!
             * 더 이상 user_info 에 바로 INSERT 하지 않고,
             * 승인 대기용 테이블 (예: signup_request)에 PENDING 상태로 적재한다.
             *
             * 테이블 예시는 대략 이런 구조라고 가정:
             *
             * create table signup_request (
             *   id           int auto_increment primary key,
             *   user_id      varchar(50)  not null,
             *   pwd          varchar(100) not null,
             *   name         varchar(100) not null,
             *   email        varchar(150) not null,
             *   department   varchar(50)  not null,
             *   requested_at datetime     default CURRENT_TIMESTAMP not null,
             *   approved_at  datetime     null,
             *   status       varchar(10)  not null default 'PENDING',
             *   unique (user_id)
             * ) charset = utf8mb4;
             */

            String sql =
                    "INSERT INTO user_signup_request " +
                            " (user_id, pwd, name, email, department, requested_at, status) " +
                            "VALUES (?, ?, ?, ?, ?, NOW(), 'PENDING')";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, password);   // 지금은 평문, 필요하면 나중에 해시
            pstmt.setString(3, userName);
            pstmt.setString(4, email);
            pstmt.setString(5, department);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                // 신청 저장까지는 성공 → "승인 대기"라고 알려주면서 로그인 화면으로
                response.sendRedirect(request.getContextPath() + "/index.jsp?signup=pending");
            } else {
                response.getWriter().println("회원가입 신청에 실패했습니다. 다시 시도해보세요.");
            }

        } catch (ClassNotFoundException e) {
            throw new ServletException("드라이버 로드 실패", e);
        } catch (SQLException e) {
            e.printStackTrace();

            // 아이디 중복(UNIQUE 위반) 같은 경우 사용자에게 좀 더 친절하게
            // MySQL: SQLState 23000, ErrorCode 1062 가 대표적인 중복 에러
            if ("23000".equals(e.getSQLState())) {
                response.getWriter().println("이미 사용 중인 아이디입니다. 다른 아이디를 입력해 주세요.");
            } else {
                response.getWriter().println("DB 에러: " + e.getMessage());
            }

        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn  != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
