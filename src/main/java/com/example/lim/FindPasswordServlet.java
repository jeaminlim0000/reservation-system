package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name="FindPasswordServlet", urlPatterns="/findPassword")
public class FindPasswordServlet extends HttpServlet {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/limlimlim?serverTimezone=UTC";
    private static final String DB_USER     = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String id   = nv(req.getParameter("userId"));
        String name = nv(req.getParameter("userName"));
        String dept = nv(req.getParameter("department"));

        if (id.length()==0 || name.length()==0 || dept.length()==0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("필수 항목이 비었습니다.");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver"); // Java 6 호환
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String sql = "SELECT pwd FROM user_info WHERE id=? AND name=? AND department=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, dept);

            rs = ps.executeQuery();
            PrintWriter out = resp.getWriter();
            if (rs.next()) {
                String pwd = rs.getString("pwd");
                out.print(mask(pwd));     // 마스킹해서 반환
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("일치하는 정보가 없습니다.");
            }

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("DB 오류: " + e.getMessage());
        } finally {
            if (rs!=null) try { rs.close(); } catch(SQLException ignore){}
            if (ps!=null) try { ps.close(); } catch(SQLException ignore){}
            if (conn!=null) try { conn.close(); } catch(SQLException ignore){}
        }
    }

    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    // 비밀번호 마스킹: 앞 2글자 + 뒤 1글자만 보여주고 나머지는 '*'
    private static String mask(String pwd) {
        if (pwd == null) return "";
        int n = pwd.length();
        if (n <= 2) return "**";                 // 너무 짧으면 전부 마스킹
        if (n == 3) return pwd.substring(0,1) + "**";
        String head = pwd.substring(0, 2);
        String tail = pwd.substring(n - 1);
        StringBuilder mid = new StringBuilder();
        for (int i = 0; i < n - 3; i++) mid.append('*');
        return head + mid + tail;
    }
}
