package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class AdminRoomListServlet extends HttpServlet {

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

    static class Row {
        public int id;
        public String campus;
        public String building;
        public int floor;
        public String roomNo;
        public String addedAt;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

//        if(!isAdmin(req)){ resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Connection conn=null; PreparedStatement ps=null; ResultSet rs=null;
        List<Row> list = new ArrayList<Row>();

        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            ps = conn.prepareStatement(
                    "SELECT id, campus, building, floor, room_no, added_at " +
                            "FROM room ORDER BY id DESC");
            rs = ps.executeQuery();

            while(rs.next()){
                Row r = new Row();
                r.id       = rs.getInt("id");
                r.campus   = rs.getString("campus");
                r.building = rs.getString("building");
                r.floor    = rs.getInt("floor");
                r.roomNo   = rs.getString("room_no");
                Timestamp ts = rs.getTimestamp("added_at");
                r.addedAt  = (ts!=null ? ts.toString() : "");
                list.add(r);
            }

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(list));

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
}
