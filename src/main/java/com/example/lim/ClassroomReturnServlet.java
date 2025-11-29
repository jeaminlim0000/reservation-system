package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class ClassroomReturnServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    static class Res { String result; int deleted; Res(String r,int d){result=r;deleted=d;} }

    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Connection conn = null;
        PreparedStatement ps = null;
        int deleted = 0;

        // 1) 예약 PK로 취소 (신규 방식)
        String resIdStr = nv(req.getParameter("resId"));

        // 2) roomId + date + timeslot 방식 (room.id를 받아서 조인 삭제)
        String roomIdStr = nv(req.getParameter("roomId"));
        String dateStr   = nv(req.getParameter("date"));
        String slot      = nv(req.getParameter("timeslot"));

        // 3) campus/building/floor/room(+roomNo) + date + timeslot 방식 (구 방식)
        String campus   = nv(req.getParameter("campus"));
        String building = nv(req.getParameter("building"));
        String floorStr = nv(req.getParameter("floor"));
        String room     = nv(req.getParameter("room"));
        if (room.length()==0) room = nv(req.getParameter("roomNo"));

        try{
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            if (resIdStr.length() > 0) {
                // 1) by reservation.id
                int id;
                try { id = Integer.parseInt(resIdStr); }
                catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad resId"); return; }

                ps = conn.prepareStatement("DELETE FROM reservation WHERE id=?");
                ps.setInt(1, id);
                deleted = ps.executeUpdate();
            }
            else if (roomIdStr.length() > 0 && dateStr.length()>0 && slot.length()>0) {
                // 2) by roomId + date + timeslot (JOIN)
                int roomId;
                try { roomId = Integer.parseInt(roomIdStr); }
                catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad roomId"); return; }

                java.sql.Date d;
                try { d = java.sql.Date.valueOf(dateStr); }
                catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad date"); return; }

                String sql =
                        "DELETE r FROM reservation r " +
                                "JOIN room rm ON rm.id=? " +
                                " AND rm.campus = r.campus " +
                                " AND rm.building = r.building " +
                                " AND rm.floor = r.floor " +
                                " AND rm.room_no = r.room " +
                                "WHERE r.reserved_date=? AND r.timeslot=?";

                ps = conn.prepareStatement(sql);
                ps.setInt(1, roomId);
                ps.setDate(2, d);
                ps.setString(3, slot);
                deleted = ps.executeUpdate();
            }
            else if (campus.length()>0 && building.length()>0 && floorStr.length()>0 && room.length()>0
                    && dateStr.length()>0 && slot.length()>0) {
                // 3) by campus/building/floor/room + date + timeslot (구 방식)
                int floor;
                try { floor = Integer.parseInt(floorStr); }
                catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad floor"); return; }

                java.sql.Date d;
                try { d = java.sql.Date.valueOf(dateStr); }
                catch(Exception e){ resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad date"); return; }

                String sql =
                        "DELETE FROM reservation " +
                                "WHERE campus=? AND building=? AND floor=? AND room=? " +
                                " AND reserved_date=? AND timeslot=?";

                ps = conn.prepareStatement(sql);
                ps.setString(1, campus);
                ps.setString(2, building);
                ps.setInt(3, floor);
                ps.setString(4, room);
                ps.setDate(5, d);
                ps.setString(6, slot);
                deleted = ps.executeUpdate();
            }
            else {
                // 어떤 파라미터도 맞지 않으면 일로
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "requires resId OR (roomId+date+timeslot) OR (campus+building+floor+room+date+timeslot)");
                return;
            }

            PrintWriter out = resp.getWriter();
            out.print(new Gson().toJson(new Res("OK", deleted)));

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
