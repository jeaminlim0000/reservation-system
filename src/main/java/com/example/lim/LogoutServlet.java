package com.example.lim;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;


public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 세션 무효화
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

//        // (선택) rememberMe용 username 쿠키 제거
//        Cookie c = new Cookie("username", "");
//        c.setMaxAge(0);
//        c.setPath(req.getContextPath());
//        resp.addCookie(c);

        // 로그인 페이지로
        resp.sendRedirect(req.getContextPath() + "/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
