package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.Gson;

public class AdminRoomMetaServlet extends HttpServlet {

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 관리자 체크가 필요하면 주석 해제하면 됬는데 따로 추가함
        // if(!isAdmin(req)){ resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String mode     = nv(req.getParameter("mode"));
        String campus   = nv(req.getParameter("campus"));
        String building = nv(req.getParameter("building"));
        String floorStr = nv(req.getParameter("floor"));

        Connection conn = null;
        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();

        try{
            // 프로젝트의 다른 서블릿과 동일하게 구 드라이버 명시
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PW);

            // ### 새 방식: 모든 옵션을 독립적으로 한 번에
            if ("all".equalsIgnoreCase(mode) || mode.length()==0) {
                Map<String,Object> meta = new LinkedHashMap<String,Object>();
                meta.put("campuses",  distinctList(conn, "SELECT DISTINCT campus   FROM room ORDER BY campus"));
                meta.put("buildings", distinctList(conn, "SELECT DISTINCT building FROM room ORDER BY building"));
                meta.put("floors",    distinctIntList(conn, "SELECT DISTINCT floor  FROM room ORDER BY floor"));
                meta.put("rooms",     distinctList(conn, "SELECT DISTINCT room_no FROM room ORDER BY room_no"));
                out.print(gson.toJson(meta));
                return;
            }

            // ### 호환 모드들(필터 있으면 적용, 없으면 전체 반환)
            if ("campuses".equalsIgnoreCase(mode)) {
                List<String> arr = distinctList(conn,
                        "SELECT DISTINCT campus FROM room ORDER BY campus");
                out.print(gson.toJson(arr));
                return;
            }

            if ("buildings".equalsIgnoreCase(mode)) {
                List<String> arr;
                if (!campus.isEmpty()) {
                    arr = distinctList(conn,
                            "SELECT DISTINCT building FROM room WHERE campus=? ORDER BY building",
                            campus);
                } else {
                    arr = distinctList(conn,
                            "SELECT DISTINCT building FROM room ORDER BY building");
                }
                out.print(gson.toJson(arr));
                return;
            }

            if ("floors".equalsIgnoreCase(mode)) {
                List<Integer> arr;
                if (!campus.isEmpty() && !building.isEmpty()) {
                    arr = distinctIntList(conn,
                            "SELECT DISTINCT floor FROM room WHERE campus=? AND building=? ORDER BY floor",
                            campus, building);
                } else if (!building.isEmpty()) {
                    arr = distinctIntList(conn,
                            "SELECT DISTINCT floor FROM room WHERE building=? ORDER BY floor",
                            building);
                } else {
                    arr = distinctIntList(conn,
                            "SELECT DISTINCT floor FROM room ORDER BY floor");
                }
                out.print(gson.toJson(arr));
                return;
            }

            if ("rooms".equalsIgnoreCase(mode)) {
                List<String> arr;
                if (!campus.isEmpty() && !building.isEmpty() && !floorStr.isEmpty()) {
                    Integer floor = parseIntSafe(floorStr);
                    if (floor == null) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid floor");
                        return;
                    }
                    arr = distinctList(conn,
                            "SELECT DISTINCT room_no FROM room WHERE campus=? AND building=? AND floor=? ORDER BY room_no",
                            campus, building, floor);
                } else {
                    arr = distinctList(conn,
                            "SELECT DISTINCT room_no FROM room ORDER BY room_no");
                }
                out.print(gson.toJson(arr));
                return;
            }

            // 알 수 없는 모드는 all과 동일 처리
            Map<String,Object> meta = new LinkedHashMap<String,Object>();
            meta.put("campuses",  distinctList(conn, "SELECT DISTINCT campus   FROM room ORDER BY campus"));
            meta.put("buildings", distinctList(conn, "SELECT DISTINCT building FROM room ORDER BY building"));
            meta.put("floors",    distinctIntList(conn, "SELECT DISTINCT floor  FROM room ORDER BY floor"));
            meta.put("rooms",     distinctList(conn, "SELECT DISTINCT room_no FROM room ORDER BY room_no"));
            out.print(gson.toJson(meta));

        } catch (ClassNotFoundException e){
            throw new ServletException("JDBC driver load failed", e);
        } catch (SQLException e){
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DB error: " + e.getMessage());
        } finally {
            if (conn!=null) try { conn.close(); } catch(SQLException ignore) {}
        }
    }

    // ===== helpers =====
    private static String nv(String s){ return (s==null) ? "" : s.trim(); }

    private static Integer parseIntSafe(String s){
        try { return Integer.valueOf(Integer.parseInt(s)); }
        catch(Exception e){ return null; }
    }

    private static List<String> distinctList(Connection conn, String sql, Object...params) throws SQLException {
        List<String> list = new ArrayList<String>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            for (int i=0;i<params.length;i++) ps.setObject(i+1, params[i]);
            rs = ps.executeQuery();
            while(rs.next()){
                String v = rs.getString(1);
                if (v != null && !v.isEmpty()) list.add(v);
            }
        } finally {
            if (rs!=null) try { rs.close(); } catch(SQLException ignore){}
            if (ps!=null) try { ps.close(); } catch(SQLException ignore){}
        }
        return list;
    }

    private static List<Integer> distinctIntList(Connection conn, String sql, Object...params) throws SQLException {
        List<Integer> list = new ArrayList<Integer>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            for (int i=0;i<params.length;i++) ps.setObject(i+1, params[i]);
            rs = ps.executeQuery();
            while(rs.next()){
                list.add(Integer.valueOf(rs.getInt(1)));
            }
        } finally {
            if (rs!=null) try { rs.close(); } catch(SQLException ignore){}
            if (ps!=null) try { ps.close(); } catch(SQLException ignore){}
        }
        return list;
    }
}
