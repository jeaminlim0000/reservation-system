package com.example.lim;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ctx = request.getContextPath();
        String uri = request.getRequestURI();

        // 로그인 없이 접근 허용 (로그인/회원가입/정적리소스)
        boolean open =
                uri.equals(ctx + "/index.jsp") ||
                        uri.equals(ctx + "/signup.jsp") ||
                        uri.startsWith(ctx + "/signup") ||     // 회원가입 서블릿
                        uri.startsWith(ctx + "/login")  ||     // 로그인 서블릿
//                        uri.startsWith(ctx + "/logout") ||     // 로그아웃 서블릿
//                        uri.startsWith(ctx + "/Logout") ||     // 대소문자 섞여 있으면 둘 다 허용
                        uri.startsWith(ctx + "/css/")   ||
                        uri.startsWith(ctx + "/js/")    ||
                        uri.startsWith(ctx + "/images/")||
                        uri.startsWith(ctx + "/assets/")||
                        uri.startsWith(ctx + "/static/")||
                        uri.equals(ctx + "/findPassword.jsp") ||
                        uri.startsWith(ctx + "/findPassword") ||
                        uri.endsWith("/favicon.ico");


        if (open) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        String logged = (session != null) ? (String) session.getAttribute("loggedInUser") : null;

        if (logged == null || logged.length() == 0) {
            // 캐시 방지 (뒤로가기 방지 효과)인데 뭔가
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            response.sendRedirect(ctx + "/index.jsp");
            return;
        }

        chain.doFilter(req, res);
    }

    public void destroy() {}
}

