<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" session="true" %>
<%
    // 세션 사용자/관리자 플래그
    Object u = (session != null) ? session.getAttribute("loggedInUser") : null;
    String loggedUser = (u != null) ? String.valueOf(u) : "";
    boolean isAdmin = "admin".equals(loggedUser);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>대시보드</title>

    <script>
        const CP = '<%=request.getContextPath()%>';
    </script>

    <style>
        /* 상단 바 */
        .topbar{
            position: sticky; top:0; z-index:1000;
            display:flex;
            justify-content:space-between;  /* 좌우로 나눔 */
            align-items:center;
            gap:10px; padding:10px 14px;
            background:#fff; border-bottom:1px solid #eee;
        }
        .top-left, .top-right{
            display:flex;
            align-items:center;
            gap:10px;
        }
        .topbar .who{ color:#555; font-size:14px; }
        .top-btn{
            padding:6px 12px; border:1px solid #ccc; border-radius:4px;
            background:#fafafa; cursor:pointer;
        }
        .top-btn:hover{ background:#f0f0f0; }

        .dashboard {
            display: grid;
            grid-template-columns: 1fr 1fr;
            grid-template-rows: auto 1fr;
            gap: 16px;
            padding: 20px;
            height: calc(100vh - 50px);
            box-sizing: border-box;
            background: #f5f5f5;
        }
        .panel {
            background: #fff;
            border: 1px solid #ddd;
            border-radius: 4px;
            display: flex;
            flex-direction: column;
            padding: 16px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            overflow: hidden;
        }
        .panel-hd {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin: 0 0 12px 0;
            border-bottom: 1px solid #eee;
            padding-bottom: 6px;
        }
        .panel-hd h2 { margin: 0; font-size: 1.2em; }
        .edit-btn {
            padding: 6px 10px;
            border: 1px solid #ccc;
            background: #fafafa;
            border-radius: 4px;
            cursor: pointer;
        }
        .edit-btn:hover { background: #f0f0f0; }

        .panel.classroom { grid-column: 1; grid-row: 1; }
        .panel.tool      { grid-column: 2; grid-row: 1; }
        .panel.history   { grid-column: 1 / span 2; grid-row: 2; }

        .table-wrap {
            flex: 1 1 auto;
            border: 1px solid #ddd;
            border-radius: 4px;
            overflow-y: auto;
            max-height: 380px;
        }
        table { width: 100%; border-collapse: separate; border-spacing: 0; }
        th, td {
            border-bottom: 1px solid #eee;
            padding: 8px 10px;
            text-align: left;
            background: #fff;
        }
        thead th {
            position: sticky; top: 0; z-index: 1;
            background: #f7f7f7;
            border-bottom: 1px solid #ddd;
        }
        tbody tr:nth-child(even){ background: #fafafa; }
    </style>
</head>
<body>


<div class="topbar">
    <div class="top-left">
        <% if (isAdmin) { %>
        <button type="button" class="top-btn" id="btnApprove">회원 승인 관리</button>
        <button type="button" class="top-btn" id="btnDeleteUser">사용자들 계정 관리</button>
        <% } %>
    </div>

    <div class="top-right">
        <span class="who"><%= loggedUser.length() > 0 ? (loggedUser + " 님") : "" %></span>
        <button type="button" class="top-btn" id="btnLogout">로그아웃</button>
    </div>
</div>

<div class="dashboard">
    <!-- 스튜디오 예약 -->
    <section class="panel classroom">
        <div class="panel-hd">
            <h2>스튜디오 예약</h2>
            <% if (isAdmin) { %>
            <button type="button" class="edit-btn" id="btnEditClassroom">수정</button>
            <% } %>
        </div>
        <jsp:include page="/classroom.jsp"/>
    </section>

    <!-- 도구 예약 -->
    <section class="panel tool">
        <div class="panel-hd">
            <h2>도구 대여 현황</h2>
            <% if (isAdmin) { %>
            <button type="button" class="edit-btn" id="btnEditTool">수정</button>
            <% } %>
        </div>
        <jsp:include page="/tool.jsp"/>
    </section>

    <!-- 과거 예약 기록 -->
    <section class="panel history">
        <div class="panel-hd">
            <h2>과거 예약 기록</h2>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th style="width: 18%">날짜</th>
                    <th style="width: 12%">구분</th>
                    <th style="width: 50%">교실/도구</th>
                    <th style="width: 20%">시간</th>
                </tr>
                </thead>
                <tbody id="historyBody"></tbody>
            </table>
        </div>
    </section>
</div>

<script>
    // 로그아웃
    (function () {
        var btnLogout = document.getElementById('btnLogout');
        if (btnLogout) {
            btnLogout.addEventListener('click', function () {
                location.href = CP + '/logout';
            });
        }
    })();

    // 회원 승인 관리 & 회원 탈퇴 (관리자 전용)
    (function () {
        var btnApprove = document.getElementById('btnApprove');
        if (btnApprove) {
            btnApprove.addEventListener('click', function () {
                // 나중에 만들 회원 승인 페이지 경로
                location.href = CP + '/admin_signup.jsp';
            });
        }

        var btnDelete = document.getElementById('btnDeleteUser');
        if (btnDelete) {
            btnDelete.addEventListener('click', function () {
                // 나중에 만들 회원 탈퇴 페이지 경로
                location.href = CP + '/admin_delete.jsp';
            });
        }
    })();

    // 관리자 전용 버튼 있을 때만 연결
    (function(){
        var btnEditClassroom = document.getElementById('btnEditClassroom');
        if (btnEditClassroom) {
            btnEditClassroom.addEventListener('click', function () {
                location.href = CP + '/room_admin.jsp';
            });
        }
        var btnEditTool = document.getElementById('btnEditTool');
        if (btnEditTool) {
            btnEditTool.addEventListener('click', function () {
                location.href = CP + '/tool_admin.jsp';
            });
        }
    })();

    // 과거 예약 기록 로드
    (function initHistory() {
        var tbody = document.getElementById('historyBody');

        function renderRows(list) {
            tbody.innerHTML = '';
            if (!list || list.length === 0) {
                var tr = document.createElement('tr');
                tr.innerHTML = '<td colspan="4">표시할 기록이 없습니다.</td>';
                tbody.appendChild(tr);
                return;
            }
            list.forEach(function(row){
                var tr = document.createElement('tr');
                tr.innerHTML =
                    '<td>' + row.date  + '</td>' +
                    '<td>' + row.type  + '</td>' +
                    '<td>' + row.item  + '</td>' +
                    '<td>' + row.start + ' ~ ' + row.end + '</td>';
                tbody.appendChild(tr);
            });
        }

        fetch(CP + '/api/history')
            .then(function(r){ if (!r.ok) throw new Error('history ' + r.status); return r.json(); })
            .then(renderRows)
            .catch(function(err){
                console.error(err);
                tbody.innerHTML = '<tr><td colspan="4">기록을 불러오지 못했습니다.</td></tr>';
            });
    })();
</script>

</body>
</html>
