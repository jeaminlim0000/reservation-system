<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>회원가입</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .row { margin: 10px 0; }
        label { display: inline-block; width: 110px; vertical-align: middle; }
        .field { display: inline-flex; align-items: center; gap: 6px; }
        input[type=text], input[type=password], input[type=email] { padding: 6px 8px; width: 240px; }
        .toggle-btn { padding: 6px 10px; cursor: pointer; }
        .hint { font-size: 12px; color: #888; margin-left: 116px; }
        .error { color: #b00020; font-size: 12px; margin-left: 116px; }
    </style>
    <script>
        // 비밀번호 보기/숨기기 토글
        function toggleView(inputId, btn) {
            var el = document.getElementById(inputId);
            if (!el) return;
            if (el.type === 'password') {
                el.type = 'text';
                btn.textContent = '숨기기';
            } else {
                el.type = 'password';
                btn.textContent = '보기';
            }
        }

        // 실시간 일치 여부 표시
        function checkMatch() {
            var p1 = document.getElementById('password').value;
            var p2 = document.getElementById('password2').value;
            var msg = document.getElementById('matchMsg');
            if (!p2) { msg.textContent=''; return; }
            if (p1 === p2) {
                msg.textContent = '비밀번호가 일치합니다.';
                msg.style.color = '#2e7d32';
            } else {
                msg.textContent = '비밀번호가 일치하지 않습니다.';
                msg.style.color = '#b00020';
            }
        }

        // 제출 시 검증
        function onSubmit(e){
            var p1 = document.getElementById('password').value;
            var p2 = document.getElementById('password2').value;
            var err = document.getElementById('submitErr');
            if (p1 !== p2) {
                e.preventDefault();
                err.textContent = '비밀번호가 서로 다릅니다. 다시 확인해 주세요.';
                return false;
            }
            err.textContent = '';
            return true;
        }
    </script>
</head>
<body>
<h1>회원가입 페이지</h1>

<form action="${pageContext.request.contextPath}/signup" method="post" onsubmit="return onSubmit(event)">
    <div class="row">
        <label for="userId">아이디:</label>
        <input type="text" name="userId" id="userId" required>
    </div>

    <div class="row">
        <label for="password">비밀번호:</label>
        <span class="field">
        <input type="password" name="password" id="password" required oninput="checkMatch()">
        <button type="button" class="toggle-btn" onclick="toggleView('password', this)">보기</button>
      </span>
    </div>

    <div class="row">
        <label for="password2">비밀번호 확인:</label>
        <span class="field">
        <input type="password" id="password2" required oninput="checkMatch()">
        <button type="button" class="toggle-btn" onclick="toggleView('password2', this)">보기</button>
      </span>
    </div>

    <div id="matchMsg" class="hint"></div>

    <div class="row">
        <label for="userName">이름:</label>
        <input type="text" name="userName" id="userName" required>
    </div>

    <div class="row">
        <label for="email">학교 이메일:</label>
        <input type="email" name="email" id="email" required>
    </div>

    <div class="row">
        <label for="department">학과:</label>
        <input type="text" name="department" id="department" placeholder="예)컴퓨터공학부" required>
    </div>

    <div id="submitErr" class="error"></div>

    <div class="row">
        <input type="submit" value="회원가입" style="width: 358px; padding:8px;">
    </div>
</form>

<br>
<a href="index.jsp">로그인 페이지로 돌아가기</a>
</body>
</html>
