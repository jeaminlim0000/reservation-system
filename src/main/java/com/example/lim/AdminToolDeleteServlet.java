package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AdminToolDeleteServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res {
        String result; Integer affected;
        Res(String r, Integer a){ this.result=r; this.affected=a; }
    }

    private static String slurp(HttpServletRequest req) throws IOException {
        BufferedReader br = req.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null){ sb.append(line); }
        return sb.toString();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        List<Integer> ids = new ArrayList<Integer>();

        String ct = req.getContentType();
        if (ct != null && ct.toLowerCase().indexOf("application/json") >= 0) {
            String body = slurp(req);
            try {
                JsonArray arr = new JsonParser()
                        .parse(body).getAsJsonObject()
                        .getAsJsonArray("ids");
                if (arr != null) {
                    for (JsonElement e : arr) {
                        try { ids.add(Integer.valueOf(e.getAsInt())); } catch(Exception ignore){}
                    }
                }
            } catch(Exception ignore){}
        } else {
            // 폼 방식 예비 처리: ids=1,2,3
            String csv = req.getParameter("ids");
            if (csv != null && csv.length() > 0) {
                String[] parts = csv.split(",");
                for (int i=0; i<parts.length; i++){
                    try { ids.add(Integer.valueOf(parts[i].trim())); } catch(Exception ignore){}
                }
            }
        }

        if (ids.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(new Gson().toJson(new Res("NO_IDS", Integer.valueOf(0))));
            return;
        }

        Connection conn = null; PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            StringBuilder sql = new StringBuilder("DELETE FROM tool WHERE id IN (");
            for (int i=0; i<ids.size(); i++){
                if (i>0) sql.append(',');
                sql.append('?');
            }
            sql.append(')');

            ps = conn.prepareStatement(sql.toString());
            for (int i=0; i<ids.size(); i++){
                ps.setInt(i+1, ids.get(i).intValue());
            }

            int n = ps.executeUpdate();
            resp.getWriter().print(new Gson().toJson(new Res("OK", Integer.valueOf(n))));

        } catch (ClassNotFoundException e){
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error: "+e.getMessage());
        } finally {
            if (ps!=null) try{ ps.close(); }catch(SQLException ignore){}
            if (conn!=null) try{ conn.close(); }catch(SQLException ignore){}
        }
    }
}
