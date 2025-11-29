<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
         import="java.time.LocalDate" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>도구 대여 현황</title>
    <style>
        body { font-family: Arial, sans-serif; padding:20px; }
        h1 { margin-bottom:20px; }
        .filter { margin-bottom:20px; }
        .filter label { margin-right:15px; }
        .filter select, .filter input { padding:4px 6px; }
        .timeslots { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:12px; }
        .timeslot { padding:8px 12px; border:1px solid #444; border-radius:4px; cursor:pointer; }
        .timeslot.booked { color:#999; text-decoration:line-through; background:#f5f5f5; cursor:not-allowed; }
        .timeslot.selected { background:#cce0ff; }
        #actionButtons { margin-bottom:20px; }
        #actionButtons button { padding:8px 16px; margin-right:8px; border:none; border-radius:4px; color:#fff; cursor:pointer; }
        #reserveToolBtn { background:#3366ff; }
        #returnBtn {  background:#dc3545;
            width:100%;
            padding:6px 0;
            border:none;
            border-radius:4px;
            color:#fff;
            cursor:pointer;
            display:block;
        }
        #reserveToolBtn:disabled, #returnBtn:disabled { background:#aaa; cursor:not-allowed; }

        #myLoan { width:260px; padding:10px; border:1px solid #ccc; border-radius:4px; background:#fff; box-shadow:0 2px 6px rgba(0,0,0,.1); }
        #myLoan h2 { margin-top:0; font-size:1.1em; }
        .loan-group { margin-bottom:12px; border:1px solid #ddd; border-radius:4px; }
        .loan-group h3 { margin:0; padding:6px 8px; background:#f7f7f7; }
        .loan-group ul { list-style:none; padding:6px 8px; margin:0; }
        .loan-group li { padding:6px; border:1px solid #888; border-radius:4px; margin-bottom:4px; cursor:pointer; }
        .loan-group li.selected { background:#eef6ff; }

        /* 안내문 스타일 */
        #toolHint { margin:8px 0 12px; color:#666; }
    </style>
</head>
<body>
<h1>도구 대여 현황</h1>

<div class="filter">
    <label>날짜:
        <input type="date" id="dateSelTool"
               min="<%=LocalDate.now()%>"
               value="<%=LocalDate.now()%>"/>
    </label>
    <label>카테고리:
        <select id="categorySel" disabled>
            <option value="">불러오는 중…</option>
        </select>
    </label>
    <label>도구:
        <select id="toolSel" disabled>
            <option value="">선택</option>
        </select>
    </label>
</div>

<div id="scheduleTool">
    <!-- 안내문 추가 -->
    <p id="toolHint">옵션을 선택하세요.</p>

    <div class="timeslots" id="timeslotToolContainer"></div>
    <div id="actionButtons">
        <button id="reserveToolBtn" disabled>대여</button>
    </div>
</div>

<div id="myLoan">
    <h2>내 대여</h2>
    <div id="loanContainer"></div>
    <button id="returnBtn" disabled>반납</button>
</div>

<script>
    (function(){
        if (typeof CP === 'undefined') {
            window.CP = '<%=request.getContextPath()%>';
        }

        const dateSel  = document.getElementById('dateSelTool');
        const catSel   = document.getElementById('categorySel');
        const toolSel  = document.getElementById('toolSel');
        const slotCont = document.getElementById('timeslotToolContainer');
        const resBtn   = document.getElementById('reserveToolBtn');
        const retBtn   = document.getElementById('returnBtn');
        const loanCont = document.getElementById('loanContainer');
        const toolHint = document.getElementById('toolHint');   // 안내문 엘리먼트

        let selectedTime   = null;
        let selectedLoanLi = null;

        const ok = r => { if(!r.ok) throw new Error('HTTP '+r.status); return r; };

        // 초기 안내문
        toolHint.textContent = '옵션을 선택하세요.';

        // 1) 카테고리 로드
        fetch(CP + '/api/tool/categories').then(ok).then(r=>r.json()).then(categories=>{
            catSel.innerHTML = '<option value="">선택</option>';
            categories.forEach(c => catSel.append(new Option(c, c)));
            catSel.disabled = false;
            toolHint.textContent = '옵션을 선택하세요.'; // 아직 도구 미선택
        }).catch(err=>{
            console.error('categories', err);
            catSel.innerHTML = '<option value="">불러오기 실패</option>';
        });

        // 1-2) 카테고리 선택 → 도구 로드
        catSel.addEventListener('change', ()=>{
            toolSel.innerHTML = '<option value="">선택</option>';
            toolSel.disabled = true;
            clearSchedule();
            toolHint.textContent = '옵션을 선택하세요.'; // 도구 아직 선택 안됨
            if (!catSel.value) return;

            fetch(CP + '/api/tool/items?category='+encodeURIComponent(catSel.value))
                .then(ok).then(r=>r.json())
                .then(list=>{
                    list.forEach(item => toolSel.append(new Option(item.name, item.id)));
                    toolSel.disabled = false;
                })
                .catch(err=>console.error('items', err));
        });

        // 2) 도구/날짜 선택 시 → 스케줄 로드
        toolSel.addEventListener('change', ()=>{
            clearSchedule();
            if (toolSel.value) {
                toolHint.textContent = '';          // 도구 선택됨 → 안내문 숨김
                loadSchedule();
            } else {
                toolHint.textContent = '옵션을 선택하세요.'; // 도구 선택 해제
            }
        });
        dateSel.addEventListener('change', ()=>{
            clearSchedule();
            if (toolSel.value) {
                toolHint.textContent = '';
                loadSchedule();
            } else {
                toolHint.textContent = '옵션을 선택하세요.';
            }
        });

        function clearSchedule(){
            slotCont.innerHTML = '';
            selectedTime = null;
            resBtn.disabled = true;
            // 카테고리/도구가 비어있으면 안내문 표시
            if (!catSel.value || !toolSel.value) {
                toolHint.textContent = '옵션을 선택하세요.';
            }
        }

        function loadSchedule(){
            const qs = '?id=' + encodeURIComponent(toolSel.value) +
                '&date=' + encodeURIComponent(dateSel.value);
            fetch(CP + '/api/tool/schedule' + qs)
                .then(ok).then(r=>r.json())
                .then(booked=>{
                    const TIMES = ['09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00'];
                    slotCont.innerHTML = '';
                    TIMES.forEach(t=>{
                        const d = document.createElement('div');
                        d.textContent = t;
                        d.className = 'timeslot' + (booked.includes(t) ? ' booked' : '');
                        d.onclick = ()=>{
                            if (d.classList.contains('booked')) return;
                            slotCont.querySelectorAll('.timeslot').forEach(x=>x.classList.remove('selected'));
                            d.classList.add('selected');
                            selectedTime = t;
                            resBtn.disabled = false;
                        };
                        slotCont.appendChild(d);
                    });
                    toolHint.textContent = ''; // 스케줄 표시 중에는 숨김
                })
                .catch(err=>console.error('schedule', err));
        }

        function loadMyLoans(){
            loanCont.innerHTML = '';
            fetch(CP + '/api/tool/loans', { method:'POST' })
                .then(ok).then(r=>r.json())
                .then(list=>{
                    list.forEach(item=>{
                        let grp = document.getElementById('grp-'+item.date);
                        if(!grp){
                            grp = document.createElement('div');
                            grp.className = 'loan-group';
                            grp.id = 'grp-'+item.date;
                            const h3 = document.createElement('h3'); h3.textContent = item.date;
                            const ul = document.createElement('ul');
                            grp.append(h3, ul);
                            loanCont.appendChild(grp);
                        }
                        const li = document.createElement('li');
                        li.textContent = item.name + ' ▶ ' + item.timeslot;
                        li.dataset.id = item.id;
                        li.dataset.date = item.date;
                        li.dataset.timeslot = item.timeslot;
                        li.onclick = ()=>{
                            loanCont.querySelectorAll('li').forEach(x=>x.classList.remove('selected'));
                            li.classList.add('selected');
                            selectedLoanLi = li;
                            retBtn.disabled = false;
                        };
                        grp.querySelector('ul').appendChild(li);
                    });
                })
                .catch(err=>console.error('loans', err));
        }

        // 대여 버튼 잠금 처리
        resBtn.onclick = ()=>{
            if(!selectedTime || !toolSel.value) return;

            const sendTime = selectedTime;
            resBtn.disabled = true;

            const body = 'id=' + encodeURIComponent(toolSel.value)
                + '&date=' + encodeURIComponent(dateSel.value)
                + '&timeslot=' + encodeURIComponent(sendTime);

            fetch(CP + '/api/tool/reserve', {
                method:'POST',
                headers:{'Content-Type':'application/x-www-form-urlencoded'},
                body
            }).then(ok).then(()=>{
                selectedTime = null;
                loadSchedule();
                loadMyLoans();
                resBtn.disabled = true;
            }).catch(err=>{
                console.error('reserve', err);
                resBtn.disabled = false;
            });
        };

        function doReturn(id, date, slot){
            const body = 'id=' + encodeURIComponent(id)
                + '&date=' + encodeURIComponent(date)
                + '&timeslot=' + encodeURIComponent(slot);
            fetch(CP + '/api/tool/return', {
                method:'POST',
                headers:{'Content-Type':'application/x-www-form-urlencoded'},
                body
            }).then(ok).then(()=>{
                loadSchedule();
                loadMyLoans();
                retBtn.disabled = true;
            }).catch(err=>console.error('return', err));
        }
        retBtn.onclick = ()=>{
            if (!selectedLoanLi) return;
            doReturn(selectedLoanLi.dataset.id, selectedLoanLi.dataset.date, selectedLoanLi.dataset.timeslot);
        };

        loadMyLoans();
    })();
</script>
</body>
</html>
