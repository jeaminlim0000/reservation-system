package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

@WebServlet("/admin/signup/list")
public class AdminSignupListServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Row {
        public int    id;
        public String userId;
        public String name;
        public String email;
        public String department;
        public String requestedAt;
        public String status;
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        String u = (s != null) ? (String) s.getAttribute("loggedInUser") : null;
        return "admin".equals(u);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!isAdmin(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        List<Row> list = new ArrayList<Row>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            String sql =
                    "SELECT req_id, user_id, name, email, department, requested_at, status " +
                            "  FROM user_signup_request " +
                            " ORDER BY requested_at DESC";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                Row r = new Row();
                r.id          = rs.getInt("req_id");
                r.userId      = rs.getString("user_id");
                r.name        = rs.getString("name");
                r.email       = rs.getString("email");
                r.department  = rs.getString("department");
                r.requestedAt = String.valueOf(rs.getTimestamp("requested_at"));
                r.status      = rs.getString("status");
                list.add(r);
            }

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(list));

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());
        } finally {
            if (rs  != null) try { rs.close();  } catch (SQLException ignored) {}
            if (ps  != null) try { ps.close();  } catch (SQLException ignored) {}
            if (conn!= null) try { conn.close();} catch (SQLException ignored) {}
        }
    }
}
