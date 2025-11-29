<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>비밀번호 찾기</title>
    <style>
        body{font-family:Arial,sans-serif;background:#f0f0f0}
        .box{width:320px;margin:80px auto;background:#fff;padding:20px;border-radius:6px;box-shadow:0 0 10px rgba(0,0,0,.1)}
        input,button{width:100%;padding:10px;margin:6px 0;box-sizing:border-box}
        .result{margin-top:10px;padding:10px;background:#fafafa;border:1px solid #ddd;border-radius:4px}
    </style>
    <script>
        function findPwd(e){
            e.preventDefault();
            var f = document.forms.fp;
            var body =
                "userId=" + encodeURIComponent(f.userId.value) +
                "&userName=" + encodeURIComponent(f.userName.value) +
                "&department=" + encodeURIComponent(f.department.value);

            fetch("<%=request.getContextPath()%>/findPassword", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8"},
                body
            }).then(function(r){
                if(!r.ok) throw r;
                return r.text();
            }).then(function(text){
                document.getElementById('result').textContent = "비밀번호 힌트: " + text;
            }).catch(function(err){
                if (err.text) {
                    err.text().then(function(t){ document.getElementById('result').textContent = t; });
                } else {
                    document.getElementById('result').textContent = "조회 실패";
                }
            });
        }
    </script>
</head>
<body>
<div class="box">
    <h3>비밀번호 찾기</h3>
    <form name="fp" onsubmit="findPwd(event)">
        <input type="text" name="userId" placeholder="아이디" required>
        <input type="text" name="userName" placeholder="이름" required>
        <input type="text" name="department" placeholder="전공(학부)" required>
        <button type="submit">조회</button>
    </form>
    <div id="result" class="result"></div>
    <button onclick="location.href='<%=request.getContextPath()%>/index.jsp'">로그인으로</button>
</div>
</body>
</html>
