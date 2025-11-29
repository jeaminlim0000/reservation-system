package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

/**
 * POST /admin/tool/add
 * 요청 파라미터: name, category, department(선택), location(선택)
 * 성공: {"result":"OK","id":<신규ID>}
 * 실패: {"result":"ERR","message":"..."}
 */
public class AdminToolAddServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://localhost:3306/limlimlim"
                    + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "YOUR_DB_USER";
    private static final String PW   = "YOUR_DB_PASSWORD";

    private static final Gson gson = new Gson();

    /** 필요 시 권한 체크 (관리자만 제한하려면 true 조건만 내비두면 됨 아님 나중에 글로벌 서블릿으로 만들든가) */
    private boolean allowed(HttpServletRequest req){
        HttpSession s = req.getSession(false);
        String u = (s!=null) ? (String)s.getAttribute("loggedInUser") : null;
        // 관리자만 허용하려면: return "admin".equals(u);
        return u != null; // 로그인 사용자라면 허용
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Map<String,Object> out = new HashMap<String,Object>();

        if(!allowed(req)){
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.put("result","ERR");
            out.put("message","forbidden");
            resp.getWriter().print(gson.toJson(out));
            return;
        }

        req.setCharacterEncoding("UTF-8");
        String name       = trim(req.getParameter("name"));
        String category   = trim(req.getParameter("category"));
        String department = trim(req.getParameter("department"));
        String location   = trim(req.getParameter("location"));

        if(isEmpty(name) || isEmpty(category)){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.put("result","ERR");
            out.put("message","name, category required");
            resp.getWriter().print(gson.toJson(out));
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try{
            Class.forName("com.mysql.jdbc.Driver"); // MySQL 옛날버전 최신은 .cj를 중간에 넣는데 옛날버전이라 없음
            conn = DriverManager.getConnection(URL, USER, PW);

            // INSERT
            ps = conn.prepareStatement(
                    "INSERT INTO tool (name, category, department, location) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, category);
            // 빈문자열은 NULL 로 저장
            if(isEmpty(department)) ps.setNull(3, Types.VARCHAR); else ps.setString(3, department);
            if(isEmpty(location))   ps.setNull(4, Types.VARCHAR); else ps.setString(4, location);

            int n = ps.executeUpdate();
            if (rs != null) try{ rs.close(); } catch(SQLException ignore){}
            rs = ps.getGeneratedKeys();

            int newId = 0;
            if (rs != null && rs.next()) {
                newId = rs.getInt(1);
            }

            out.put("result", n>0 ? "OK" : "ERR");
            if (n>0) out.put("id", newId);
            resp.getWriter().print(gson.toJson(out));

        } catch (ClassNotFoundException e){
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.put("result","ERR");
            out.put("message","JDBC driver load failed");
            resp.getWriter().print(gson.toJson(out));
        } catch (SQLException e){
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.put("result","ERR");
            out.put("message","DB error: " + e.getMessage());
            resp.getWriter().print(gson.toJson(out));
        } finally {
            if (rs != null) try{ rs.close(); } catch(SQLException ignore){}
            if (ps != null) try{ ps.close(); } catch(SQLException ignore){}
            if (conn != null) try{ conn.close(); } catch(SQLException ignore){}
        }
    }

    private static String trim(String s){ return s==null?null:s.trim(); }
    private static boolean isEmpty(String s){ return s==null || s.trim().isEmpty(); }
}
