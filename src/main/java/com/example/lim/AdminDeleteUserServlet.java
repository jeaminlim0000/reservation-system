package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

public class AdminDeleteUserServlet extends HttpServlet {


    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    private boolean isAdmin(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        String u = (s!=null) ? (String)s.getAttribute("loggedInUser") : null;
        return "admin".equals(u);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 관리자 체크 (필요하면 주석 해제해서 사용)
        // if(!isAdmin(req)){
        //     resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        //     return;
        // }

        req.setCharacterEncoding("UTF-8");

        String id = req.getParameter("id");
        if (id == null || id.trim().length() == 0) {
            // 잘못된 요청이면 다시 목록 페이지로 보냄
            resp.sendRedirect(req.getContextPath() + "/admin_delete.jsp");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            // 드라이버명도 AdminRoomAddServlet 과 동일
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            ps = conn.prepareStatement("DELETE FROM user_info WHERE id = ?");
            ps.setString(1, id.trim());
            ps.executeUpdate();

            // 삭제 후 목록 페이지로 리다이렉트
            resp.sendRedirect(req.getContextPath() + "/admin_delete.jsp");

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error: " + e.getMessage());
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignore) {}
        }
    }
}
