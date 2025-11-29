package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class AdminRoomUpdateServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res { String result; int updated; String message;
        Res(String r,int u){result=r;updated=u;}
        Res(String r,int u,String m){result=r;updated=u;message=m;}
    }

    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 관리자 체크가 필요하면 주석 해제하면 되는데 얘도 따로 추가했음
        // HttpSession s = req.getSession(false);
        // if (s==null || !"admin".equals(s.getAttribute("loggedInUser"))) {
        //     resp.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        // }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String idStr    = nv(req.getParameter("id"));
        String campus   = nv(req.getParameter("campus"));
        String building = nv(req.getParameter("building"));
        String floorStr = nv(req.getParameter("floor"));
        String roomNo   = nv(req.getParameter("roomNo"));

        // 기본 유효성
        if (idStr.length()==0 || campus.length()==0 || building.length()==0 || floorStr.length()==0 || roomNo.length()==0){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(new Gson().toJson(new Res("ERR", 0, "all fields required")));
            return;
        }
        // 길이 가드(스키마에 맞춤)
        if (roomNo.length() > 20){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(new Gson().toJson(new Res("ERR", 0, "roomNo too long")));
            return;
        }

        int id, floor;
        try { id = Integer.parseInt(idStr); }
        catch(Exception e){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(new Gson().toJson(new Res("ERR", 0, "bad id")));
            return;
        }
        try { floor = Integer.parseInt(floorStr); }
        catch(Exception e){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print(new Gson().toJson(new Res("ERR", 0, "floor must be number")));
            return;
        }

        Connection conn=null; PreparedStatement ps=null;
        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            String sql = "UPDATE room SET campus=?, building=?, floor=?, room_no=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, campus);
            ps.setString(2, building);
            ps.setInt(3, floor);
            ps.setString(4, roomNo);
            ps.setInt(5, id);

            int n = ps.executeUpdate();
            resp.getWriter().print(new Gson().toJson(new Res("OK", n)));

        } catch (ClassNotFoundException e){
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e){
            // 중복키(UNIQUE 제약) 충돌인지 판별
            if (e.getErrorCode() == 1062) { // MySQL Duplicate entry
                resp.setStatus(409); // Conflict
                resp.getWriter().print(new Gson().toJson(new Res("DUPLICATE", 0, "duplicate campus/building/floor/room")));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().print(new Gson().toJson(new Res("ERR", 0, "DB error: " + e.getMessage())));
            }
        } finally {
            if(ps!=null) try{ps.close();}catch(SQLException ignore){}
            if(conn!=null) try{conn.close();}catch(SQLException ignore){}
        }
    }
}
