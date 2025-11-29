<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<%
    Object u = (session != null) ? session.getAttribute("loggedInUser") : null;
    boolean isAdmin = (u != null) && "admin".equals(String.valueOf(u));
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>회원가입 승인 관리</title>
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
        body{font-family:Arial, sans-serif; padding:20px;}
        h1{margin-bottom:12px;}
        .muted{color:#777}
        table{width:100%; border-collapse:collapse;}
        th,td{border-bottom:1px solid #eee; padding:8px 10px; text-align:left;}
        thead th{background:#f7f7f7; border-bottom:1px solid #ddd;}
        .btn{padding:6px 10px; border:1px solid #ccc; background:#fafafa; border-radius:4px; cursor:pointer}
        .btn:hover{background:#f0f0f0}
        .tag{display:inline-block; padding:2px 6px; border:1px solid #ddd; border-radius:4px; font-size:12px}
    </style>
    <script>const CP = '<%=request.getContextPath()%>';</script>
</head>
<body>
<div class="back-btn-wrap">
    <button type="button" class="back-btn"
            onclick="location.href='<%=request.getContextPath()%>/main.jsp'">
        <- 뒤로가기
    </button>
</div>

<h1>회원가입 승인 관리</h1>

<% if (!isAdmin) { %>
<p class="muted">관리자만 접근할 수 있습니다.</p>
<% } else { %>
<table>
    <thead>
    <tr>
        <th>ID</th>
        <th>아이디</th>
        <th>이름</th>
        <th>이메일</th>
        <th>학과</th>
        <th>요청시각</th>
        <th>상태</th>
        <th style="width:180px">작업</th>
    </tr>
    </thead>
    <tbody id="tbody"><tr><td colspan="8" class="muted">불러오는 중…</td></tr></tbody>
</table>
<p class="muted" id="msg"></p>
<% } %>

<script>
    (function(){
        if (!<%=isAdmin%>) return;

        const tbody = document.getElementById('tbody');
        const msg = document.getElementById('msg');

        function setMsg(t){ msg.textContent = t || ''; }

        function rowHtml(r){
            const actionBtns = (r.status === 'PENDING')
                ? '<button class="btn approve" data-id="'+r.id+'">승인</button> '+
                '<button class="btn reject"  data-id="'+r.id+'">거절</button>'
                : '<span class="tag">'+r.status+'</span>';

            return '<tr>'+
                '<td>'+r.id+'</td>'+
                '<td>'+r.userId+'</td>'+
                '<td>'+r.name+'</td>'+
                '<td>'+r.email+'</td>'+
                '<td>'+r.department+'</td>'+
                '<td>'+r.requestedAt+'</td>'+
                '<td>'+r.status+'</td>'+
                '<td>'+actionBtns+'</td>'+
                '</tr>';
        }

        function load(){
            setMsg('목록 불러오는 중…');
            fetch(CP + '/admin/signup/list', {headers:{'Accept':'application/json'}, cache:'no-store'})
                .then(r=>{ if(!r.ok) throw new Error(r.status); return r.json(); })
                .then(list=>{
                    tbody.innerHTML = '';
                    if(!list || !list.length){
                        tbody.innerHTML = '<tr><td colspan="8" class="muted">대기 중인 요청이 없습니다.</td></tr>';
                    }else{
                        list.forEach(r=>{ tbody.insertAdjacentHTML('beforeend', rowHtml(r)); });
                    }
                    setMsg('');
                })
                .catch(e=>{
                    tbody.innerHTML = '<tr><td colspan="8" class="muted">목록을 불러오지 못했습니다.</td></tr>';
                    setMsg('에러: '+e.message);
                });
        }

        tbody.addEventListener('click', (e)=>{
            const t = e.target;
            if(!t.classList.contains('approve') && !t.classList.contains('reject')) return;
            const id = t.getAttribute('data-id');
            const action = t.classList.contains('approve') ? 'approve' : 'reject';
            const form = new URLSearchParams({id, action});
            setMsg('처리 중…');

            fetch(CP + '/admin/signup/approve', {
                method:'POST',
                headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8', 'Accept':'application/json'},
                body: form.toString()
            })
                .then(r=>r.json())
                .then(j=>{
                    if(j.result === 'OK'){
                        setMsg(j.message || '완료');
                        load();
                    }else{
                        setMsg(j.message || '처리 실패');
                    }
                })
                .catch(e=> setMsg('에러: '+e.message));
        });

        load();
    })();
    // 여기서 ()는 로드되자마자 한 번 실행되는 용
    // 이 () 를 빼면 함수는 만들어지기만 하고 안에 있는 코드는 실행되지 안흠
</script>
</body>
</html>
