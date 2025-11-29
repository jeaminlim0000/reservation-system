package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class AdminRoomDeleteServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res { String result; int deleted; Res(String r,int d){result=r;deleted=d;} }
    static class Req { List<Integer> ids; }   // JSON용

    private boolean isAdmin(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        String u = (s!=null) ? (String)s.getAttribute("loggedInUser") : null;
        return "admin".equals(u);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // if(!isAdmin(req)){ resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        // --- 입력 파싱: JSON 우선, 없으면 폼 파라미터(ids=1,2,3) ---
        List<Integer> ids = new ArrayList<Integer>();
        String ct = req.getContentType();
        try {
            if (ct != null && ct.toLowerCase().startsWith("application/json")) {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = req.getReader();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                Req r = new Gson().fromJson(sb.toString(), Req.class);
                if (r != null && r.ids != null) ids.addAll(r.ids);
            } else {
                String idsParam = req.getParameter("ids");
                if (idsParam != null) {
                    String[] parts = idsParam.split(",");
                    for (int i=0;i<parts.length;i++) {
                        String p = parts[i].trim();
                        if (p.length()==0) continue;
                        try { ids.add(Integer.valueOf(Integer.parseInt(p))); } catch(Exception ignore){}
                    }
                }
            }
        } catch(Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid body");
            return;
        }

        if (ids.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ids required");
            return;
        }

        Connection conn=null; PreparedStatement ps=null;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            StringBuilder sql = new StringBuilder("DELETE FROM room WHERE id IN (");
            for(int i=0;i<ids.size();i++){
                if(i>0) sql.append(',');
                sql.append('?');
            }
            sql.append(')');

            ps = conn.prepareStatement(sql.toString());
            for(int i=0;i<ids.size();i++){
                ps.setInt(i+1, ids.get(i).intValue());
            }
            int n = ps.executeUpdate();

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(new Res("OK", n)));

        } catch (ClassNotFoundException e){
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error: "+e.getMessage());
        } finally {
            if(ps!=null) try{ps.close();}catch(SQLException ignore){}
            if(conn!=null) try{conn.close();}catch(SQLException ignore){}
        }
    }
}
