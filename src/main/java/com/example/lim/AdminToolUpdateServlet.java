package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import com.google.gson.Gson;

public class AdminToolUpdateServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res { String result; Integer affected; Res(String r,Integer a){result=r;affected=a;} }
    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String idStr      = nv(req.getParameter("id"));
        String name       = nv(req.getParameter("name"));
        String category   = nv(req.getParameter("category"));
        String department = nv(req.getParameter("department"));
        String location   = nv(req.getParameter("location"));

        int id;
        try { id = Integer.parseInt(idStr); }
        catch(Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id required"); return;
        }
        if (name.length()==0 || category.length()==0){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "name & category required"); return;
        }

        Connection conn=null; PreparedStatement ps=null;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            String sql = "UPDATE tool SET name=?, category=?, department=?, location=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, department);
            ps.setString(4, location);
            ps.setInt(5, id);

            int n = ps.executeUpdate();
            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(new Res("OK", Integer.valueOf(n))));

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
