<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" import="java.sql.*" %>
<%
    String loggedUser = (session != null) ? (String)session.getAttribute("loggedInUser") : null;
    if (!"admin".equals(loggedUser)) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>회원 탈퇴 관리</title>
    <style>
        .back-btn-wrap {
            position: fixed;
            left: 16px;
            bottom: 16px;
            z-index: 1000;
        }
        .back-btn {
            padding: 6px 14px;
            border: 1px solid #ccc;
            border-radius: 4px;
            background: #fafafa;
            cursor: pointer;
            font-size: 13px;
        }
        .back-btn:hover {
            background: #f0f0f0;
        }
        table { border-collapse:collapse; width:100%; }
        th, td { border:1px solid #ddd; padding:6px 8px; }
        thead { background:#f7f7f7; }

        .btn-delete{
            display:block;
            width:80%;
            padding:6px 0;
            margin:4px auto;
            box-sizing:border-box;
        }
    </style>
</head>
<body>
<div class="back-btn-wrap">
    <button type="button" class="back-btn"
            onclick="location.href='<%=request.getContextPath()%>/main.jsp'">
        <- 뒤로가기
    </button>
</div>
<h1>회원 탈퇴 관리</h1>

<table>
    <thead>
    <tr>
        <th>ID</th>
        <th>이름</th>
        <th>이메일</th>
        <th>학과</th>
        <th>가입일</th>
        <th>탈퇴</th>
    </tr>
    </thead>
    <tbody>
    <%
        Class.forName("com.mysql.jdbc.Driver");
        String url  = "jdbc:mysql://localhost:3306/limlimlim"
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
        String user = "YOUR_DB_USER";
        String pwd  = "YOUR_DB_PASSWORD";

        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, email, department, reg_date " +
                             "FROM user_info WHERE id != 'admin' ORDER BY reg_date DESC");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id    = rs.getString("id");
                String name  = rs.getString("name");
                String email = rs.getString("email");
                String dept  = rs.getString("department");
                Timestamp regDate = rs.getTimestamp("reg_date");
    %>
    <tr>
        <td><%=id%></td>
        <td><%=name%></td>
        <td><%=email%></td>
        <td><%=dept%></td>
        <td><%=regDate%></td>
        <td>
            <form method="post" action="<%=request.getContextPath()%>/admin/deleteUser">
                <input type="hidden" name="id" value="<%=id%>">
                <button type="submit"
                        class="btn-delete"
                        onclick="return confirm('<%=id%> 회원을 정말 탈퇴시키겠습니까?');">
                    탈퇴
                </button>
            </form>
        </td>
    </tr>
    <%
            }
        }
    %>
    </tbody>
</table>

</body>
</html>
