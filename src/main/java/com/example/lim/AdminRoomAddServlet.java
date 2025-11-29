package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class AdminRoomAddServlet extends HttpServlet {

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

    static class Res { String result; Integer id; Res(String r,Integer i){result=r;id=i;} }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

//        if(!isAdmin(req)){ resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String campus   = nv(req.getParameter("campus"));
        String building = nv(req.getParameter("building"));
        String floorStr = nv(req.getParameter("floor"));
        String roomNo   = nv(req.getParameter("roomNo"));

        if(campus.length()==0 || building.length()==0 || floorStr.length()==0 || roomNo.length()==0){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "all fields required");
            return;
        }

        int floor;
        try { floor = Integer.parseInt(floorStr); }
        catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "floor must be number"); return; }

        Connection conn=null; PreparedStatement ps=null; ResultSet rs=null;

        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            // 1) 이미 존재하는지 확인 (중복 방지)
            ps = conn.prepareStatement(
                    "SELECT id FROM room WHERE campus=? AND building=? AND floor=? AND room_no=?");
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, roomNo);
            rs = ps.executeQuery();
            if(rs.next()){
                int existingId = rs.getInt(1);
                close(rs, ps);
                PrintWriter out = resp.getWriter();
                out.print(new Gson().toJson(new Res("OK", Integer.valueOf(existingId))));
                return;
            }
            close(rs, ps);

            // 2) 신규 삽입
            ps = conn.prepareStatement(
                    "INSERT INTO room (campus, building, floor, room_no) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, roomNo);
            int n = ps.executeUpdate();
            Integer id = null;
            if(n>0){
                rs = ps.getGeneratedKeys();
                if(rs.next()) id = Integer.valueOf(rs.getInt(1));
            }

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(new Res("OK", id)));

        } catch (ClassNotFoundException e){
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error: "+e.getMessage());
        } finally {
            if(rs!=null) try{rs.close();}catch(SQLException ignore){}
            if(ps!=null) try{ps.close();}catch(SQLException ignore){}
            if(conn!=null) try{conn.close();}catch(SQLException ignore){}
        }
    }

    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    private static void close(ResultSet rs, Statement st){
        if(rs!=null) try{rs.close();}catch(SQLException ignore){}
        if(st!=null) try{st.close();}catch(SQLException ignore){}
    }
}
