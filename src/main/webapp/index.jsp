<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="javax.servlet.http.Cookie" %>
<!DOCTYPE html>
<html>
<head>
    <title>로그인 페이지</title>
    <style>
        body { font-family: Arial, sans-serif; background-color: #f0f0f0; }
        .container { width:300px; margin:80px auto; background:#fff; padding:20px; border-radius:5px; box-shadow:0 0 10px rgba(0,0,0,0.1); }
        input[type="text"], input[type="password"] { width:90%; padding:10px; margin:10px 0; }
        input[type="submit"], button { width:100%; padding:10px; margin:5px 0; cursor:pointer; }
        .options { display:flex; justify-content:space-between; align-items:center; }
        .error { color:red; text-align:center; margin-bottom:10px; }
    </style>
</head>
<body>
<div class="container">
    <h2>로그인</h2>

    <%  // 쿠키에서 저장된 아이디 꺼내기
        String savedUsername = "";
        Cookie[] cookies = request.getCookies();
        if (cookies!=null) {
            for (Cookie c: cookies) {
                if ("username".equals(c.getName())) {
                    savedUsername = c.getValue();
                    break;
                }
            }
        }
    %>

    <%  // 로그인 실패 메시지
        if (request.getParameter("error") != null) { %>
    <div class="error">아이디 또는 비밀번호가 틀렸습니다.</div>
    <% } %>

    <!--  form action 에 컨텍스트 경로 포함 -->
    <form action="${pageContext.request.contextPath}/login" method="post">
        <input type="text"     name="username" placeholder="아이디"   value="<%= savedUsername %>" required>
        <input type="password" name="password" placeholder="비밀번호" required>
        <div class="options">
            <label>
                <input type="checkbox" name="rememberMe"
                    <% if (!savedUsername.isEmpty()) { %> checked <% } %>
                > 아이디 기억하기
            </label>
        </div>
        <input type="submit" value="로그인">
    </form>

    <form action="findPassword.jsp" method="get">
        <button type="submit">비밀번호 찾기</button>
    </form>
    <form action="signup.jsp" method="get">
        <button type="submit">회원가입</button>
    </form>
</div>
</body>
</html>
