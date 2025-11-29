package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

public class ToolItemsServlet extends HttpServlet {
    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC";
    private static final String USER     = "YOUR_DB_USER";
    private static final String PW       = "YOUR_DB_PASSWORD";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        PrintWriter       out;

        resp.setContentType("application/json; charset=UTF-8");
        out = resp.getWriter();

        String category = req.getParameter("category");
        if (category == null || category.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "category 파라미터가 필요합니다.");
            return;
        }

        try {
            // JDBC 드라이버 로드
            Class.forName("com.mysql.jdbc.Driver");

            // 커넥션 생성
            conn = DriverManager.getConnection(URL, USER, PW);

            // 쿼리 준비 및 실행
            String sql = "SELECT id, name FROM tool WHERE category = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, category);
            rs = ps.executeQuery();

            // 결과를 List<Map>에 담기
            List<Map<String,Object>> items = new ArrayList<Map<String,Object>>();
            while (rs.next()) {
                Map<String,Object> map = new HashMap<String,Object>();
                map.put("id",   rs.getInt("id"));
                map.put("name", rs.getString("name"));
                items.add(map);
            }

            // JSON으로 직렬화 후 출력
            String json = new Gson().toJson(items);
            out.print(json);

        } catch (ClassNotFoundException e) {
            throw new ServletException("JDBC 드라이버 로드 실패", e);

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB 오류: " + e.getMessage());

        } finally {
            // 리소스 해제
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
}
