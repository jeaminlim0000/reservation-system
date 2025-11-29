<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String u = (String)session.getAttribute("loggedInUser");
    if (u == null || !"admin".equals(u)) {
        response.sendError(403);  // 또는 response.sendRedirect(request.getContextPath()+"/index.jsp");
        return;
    }
%>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>도구 관리(관리자)</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        h1 { margin-bottom: 16px; }
        fieldset { border: 1px solid #ddd; border-radius: 4px; padding: 12px; margin-bottom: 16px; }
        label { display: inline-block; min-width: 110px; margin: 6px 0; }
        input[type=text], select { width: 260px; padding: 6px 8px; }
        .inline { display:inline-flex; align-items:center; gap:8px; }
        .custom-input { display:none; }
        button { padding: 8px 14px; margin-top: 10px; cursor:pointer; }
        .row { margin-bottom: 10px; }
        .wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .panel { background:#fff; border:1px solid #ddd; border-radius:4px; padding:12px; }
        .table-wrap { border:1px solid #ddd; border-radius:4px; overflow-y:auto; max-height:420px; }
        table { width:100%; border-collapse:separate; border-spacing:0; }
        th, td { border-bottom:1px solid #eee; padding:8px 10px; text-align:left; background:#fff; }
        thead th{ position:sticky; top:0; background:#f7f7f7; border-bottom:1px solid #ddd; z-index:1; }
        tbody tr:nth-child(even){ background:#fafafa; }
        .muted { color:#777; font-size:12px; }
        .error { color:#b00020; font-size:12px; margin-left:8px; }

        /* 툴바 */
        .toolbar { display:flex; justify-content:space-between; align-items:center; margin: 0 0 8px 0; }
        .toolbar .right { display:flex; gap:8px; }

        /* 모달 */
        .modal-backdrop {
            position: fixed; inset: 0; background: rgba(0,0,0,.35);
            display: none; align-items: center; justify-content: center; z-index: 9999;
        }
        .modal {
            background:#fff; border-radius:8px; min-width:420px; max-width:90vw; padding:16px;
            box-shadow:0 10px 30px rgba(0,0,0,.25);
        }
        .modal h3 { margin:0 0 12px 0; }
        .modal .row label { min-width: 90px; }
        .modal .actions { display:flex; gap:8px; justify-content:flex-end; margin-top:10px; }
    </style>
    <script>
        (function ensureCP(){
            if (typeof window.CP === 'string' && window.CP.length) return;
            var seg = location.pathname.split('/').filter(Boolean);
            window.CP = seg.length ? ('/' + seg[0]) : '';
        })();
    </script>
</head>
<body>
<h1>도구 관리 (관리자)</h1>

<div class="wrap">
    <!-- 좌측: 입력 -->
    <section class="panel">
        <h3>도구 추가</h3>
        <fieldset>
            <div class="row">
                <label>이름</label>
                <span class="inline">
                    <select id="nameSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="nameCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>카테고리</label>
                <span class="inline">
                    <select id="categorySel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="categoryCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>전공(학부)</label>
                <span class="inline">
                    <select id="deptSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="deptCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>위치</label>
                <span class="inline">
                    <select id="locSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="locCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <button id="addBtn" type="button">추가</button>
            <span id="addMsg" class="muted"></span>
            <div class="muted" style="margin-top:6px">
                이름/카테고리/전공/위치는 목록에서 선택하거나, 마지막의 <b>직접 쓰기</b>를 선택해 새 값을 입력할 수 있습니다.
            </div>
        </fieldset>
    </section>

    <!-- 우측: 목록 -->
    <section class="panel">
        <div class="toolbar">
            <h3 style="margin:0">도구 목록</h3>
            <div class="right">
                <button id="btnBulkDelete" disabled>선택 삭제</button>
            </div>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th style="width:40px"><input type="checkbox" id="chkAll"></th>
                    <th style="width:7%">ID</th>
                    <th style="width:26%">이름</th>
                    <th style="width:16%">카테고리</th>
                    <th style="width:17%">전공</th>
                    <th style="width:18%">위치</th>
                    <th style="width:10%">추가일</th>
                    <th style="width:70px">편집</th>
                </tr>
                </thead>
                <tbody id="toolBody">
                <tr><td colspan="8" class="muted">불러오는 중…</td></tr>
                </tbody>
            </table>
        </div>
    </section>
</div>

<!-- 편집 모달 -->
<div class="modal-backdrop" id="editBack">
    <div class="modal">
        <h3>도구 편집</h3>
        <form id="editForm">
            <input type="hidden" name="id" id="editId">

            <div class="row">
                <label>이름</label>
                <span class="inline">
                    <select id="editNameSel"></select>
                    <input type="text" id="editNameCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>카테고리</label>
                <span class="inline">
                    <select id="editCatSel"></select>
                    <input type="text" id="editCatCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>전공</label>
                <span class="inline">
                    <select id="editDeptSel"></select>
                    <input type="text" id="editDeptCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>위치</label>
                <span class="inline">
                    <select id="editLocSel"></select>
                    <input type="text" id="editLocCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="actions">
                <button type="button" id="btnEditCancel">취소</button>
                <button type="submit" id="btnEditSave">저장</button>
            </div>
        </form>
        <div id="editMsg" class="muted"></div>
    </div>
</div>

<script>
    (function(){
        const $ = s => document.querySelector(s);

        // 입력 컴포넌트
        const nameSel = $('#nameSel');   const nameCustom = $('#nameCustom');
        const catSel  = $('#categorySel'); const catCustom  = $('#categoryCustom');
        const deptSel = $('#deptSel');     const deptCustom = $('#deptCustom');
        const locSel  = $('#locSel');      const locCustom  = $('#locCustom');
        const addBtn = $('#addBtn'); const addMsg = $('#addMsg');

        // 목록 표/툴바
        const bodyEl = $('#toolBody');
        const chkAll = $('#chkAll');
        const btnBulkDelete = $('#btnBulkDelete');

        // 편집 모달 요소
        const editBack = $('#editBack');
        const editForm = $('#editForm');
        const editMsg  = $('#editMsg');
        const editId   = $('#editId');
        const editNameSel = $('#editNameSel'); const editNameCustom = $('#editNameCustom');
        const editCatSel  = $('#editCatSel');  const editCatCustom  = $('#editCatCustom');
        const editDeptSel = $('#editDeptSel'); const editDeptCustom = $('#editDeptCustom');
        const editLocSel  = $('#editLocSel');  const editLocCustom  = $('#editLocCustom');
        const btnEditCancel = $('#btnEditCancel');

        // ====== 공통 유틸 ======
        function setMsg(el, text, isErr){ el.textContent = text || ''; el.className = isErr ? 'error' : 'muted'; }

        function fillSelect(sel, values, placeholder){
            sel.innerHTML = '';
            const opt0 = document.createElement('option');
            opt0.value = '';
            opt0.textContent = placeholder || '선택';
            sel.appendChild(opt0);

            (values || []).forEach(v=>{
                if(!v) return;
                const o = document.createElement('option');
                o.value = v; o.textContent = v;
                sel.appendChild(o);
            });

            const custom = document.createElement('option');
            custom.value = '__CUSTOM__';
            custom.textContent = '직접 쓰기';
            sel.appendChild(custom);

            sel.disabled = false;
        }

        function bindCustomToggle(sel, input){
            sel.addEventListener('change', ()=>{
                const isCustom = sel.value === '__CUSTOM__';
                input.style.display = isCustom ? 'inline-block' : 'none';
                if (isCustom) input.focus();
            });
        }
        // 입력 폼 토글
        [ [nameSel,nameCustom], [catSel,catCustom], [deptSel,deptCustom], [locSel,locCustom] ]
            .forEach(([s,i])=>bindCustomToggle(s,i));
        // 편집 모달 토글
        [ [editNameSel,editNameCustom], [editCatSel,editCatCustom], [editDeptSel,editDeptCustom], [editLocSel,editLocCustom] ]
            .forEach(([s,i])=>bindCustomToggle(s,i));

        function readValue(sel, input){
            return (sel.value === '__CUSTOM__') ? (input.value||'').trim() : (sel.value||'').trim();
        }

        async function fetchJson(url, opts){
            const r = await fetch(url, Object.assign({ headers:{'Accept':'application/json'}, cache:'no-store' }, opts||{}));
            if(!r.ok) throw new Error(url + ' ' + r.status);
            const ct = r.headers.get('content-type') || '';
            if(ct.indexOf('application/json') === -1){
                const t = await r.text(); throw new Error('Unexpected: ' + t.slice(0,100));
            }
            return r.json();
        }

        // ====== 데이터 로드 ======
        async function loadMeta(){
            try{
                const meta = await fetchJson(CP + '/admin/tool/meta');
                fillSelect(catSel,  meta.categories,  '카테고리 선택');
                fillSelect(deptSel, meta.departments, '전공 선택');
                fillSelect(locSel,  meta.locations,   '위치 선택');

                // 편집 모달도 동일 옵션 사용
                fillSelect(editCatSel,  meta.categories,  '카테고리 선택');
                fillSelect(editDeptSel, meta.departments, '전공 선택');
                fillSelect(editLocSel,  meta.locations,   '위치 선택');
            }catch(e){
                console.error('meta load fail:', e);
                // 실패 시에도 ‘직접 쓰기’ 옵션만 제공
                [catSel,deptSel,locSel,editCatSel,editDeptSel,editLocSel].forEach(s=>fillSelect(s, [], s===catSel||s===editCatSel?'카테고리 선택':(s===deptSel||s===editDeptSel?'전공 선택':'위치 선택')));
                setMsg(addMsg, '옵션 불러오기 실패 — 직접 쓰기를 사용하세요.', true);
            }
        }

        async function loadNames(){
            try{
                const list = await fetchJson(CP + '/admin/tool/list');
                const names = Array.from(new Set((list||[]).map(x=>x.name).filter(Boolean)));
                fillSelect(nameSel,     names, '이름 선택');
                fillSelect(editNameSel, names, '이름 선택');
            }catch(e){
                console.error('names load fail:', e);
                fillSelect(nameSel, [], '이름 선택');
                fillSelect(editNameSel, [], '이름 선택');
            }
        }

        async function loadList(){
            try{
                const list = await fetchJson(CP + '/admin/tool/list');
                bodyEl.innerHTML = '';
                if(!list || !list.length){
                    bodyEl.innerHTML = '<tr><td colspan="8" class="muted">등록된 도구가 없습니다.</td></tr>';
                    return;
                }
                list.forEach(row=>{
                    const tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td><input type="checkbox" class="rowChk" data-id="'+row.id+'"></td>'+
                        '<td>'+row.id+'</td>'+
                        '<td>'+row.name+'</td>'+
                        '<td>'+row.category+'</td>'+
                        '<td>'+(row.department||'')+'</td>'+
                        '<td>'+(row.location||'')+'</td>'+
                        '<td>'+(row.addedAt||'')+'</td>'+
                        '<td><button class="editRow" data-id="'+row.id+'" '+
                        'data-name="'+(row.name||'')+'" '+
                        'data-category="'+(row.category||'')+'" '+
                        'data-dept="'+(row.department||'')+'" '+
                        'data-location="'+(row.location||'')+'">편집</button></td>';
                    bodyEl.appendChild(tr);
                });
            }catch(e){
                console.error(e);
                bodyEl.innerHTML = '<tr><td colspan="8" class="error">목록을 불러오지 못했습니다.</td></tr>';
            }
        }

        // ====== 추가 ======
        addBtn.addEventListener('click', async ()=>{
            const name = readValue(nameSel, nameCustom);
            const category   = readValue(catSel,  catCustom);
            const department = readValue(deptSel, deptCustom);
            const location   = readValue(locSel,  locCustom);

            if(!name || !category){
                setMsg(addMsg, '이름과 카테고리는 필수입니다.', true);
                return;
            }

            setMsg(addMsg, '추가 중…');
            addBtn.disabled = true;

            const body = new URLSearchParams({ name, category, department, location }).toString();

            try{
                const res = await fetchJson(CP + '/admin/tool/add', {
                    method: 'POST',
                    headers: { 'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8' },
                    body
                });
                if(res && res.result === 'OK'){
                    setMsg(addMsg, '추가 완료 (ID ' + (res.id || '?') + ')');
                    [nameSel, catSel, deptSel, locSel].forEach(s=>{ s.value=''; });
                    [nameCustom, catCustom, deptCustom, locCustom].forEach(i=>{ i.value=''; i.style.display='none'; });
                    await loadNames(); await loadMeta(); await loadList();
                }else{
                    setMsg(addMsg, '추가 실패', true);
                }
            }catch(e){
                console.error(e);
                setMsg(addMsg, '추가 중 오류가 발생했습니다.', true);
            }finally{
                addBtn.disabled = false;
            }
        });

        // ====== 체크박스/선택 삭제 ======
        function updateBulkDeleteState(){
            const any = !!bodyEl.querySelector('.rowChk:checked');
            btnBulkDelete.disabled = !any;
            // 헤더 전체선택 상태 업데이트
            const all  = bodyEl.querySelectorAll('.rowChk').length;
            const on   = bodyEl.querySelectorAll('.rowChk:checked').length;
            chkAll.checked = (all>0 && on===all);
        }
        chkAll.addEventListener('change', ()=>{
            bodyEl.querySelectorAll('.rowChk').forEach(c=>c.checked = chkAll.checked);
            updateBulkDeleteState();
        });
        bodyEl.addEventListener('change', e=>{
            if(e.target.classList.contains('rowChk')) updateBulkDeleteState();
        });
        btnBulkDelete.addEventListener('click', async ()=>{
            const ids = [...bodyEl.querySelectorAll('.rowChk:checked')].map(c=>parseInt(c.dataset.id,10));
            if(!ids.length) return;
            if(!confirm(ids.length + '개 항목을 삭제하시겠습니까?')) return;
            try{
                await fetchJson(CP + '/admin/tool/delete', {
                    method:'POST',
                    headers:{'Content-Type':'application/json'},
                    body: JSON.stringify({ ids })
                });
                await loadList();
                updateBulkDeleteState();
            }catch(e){
                alert('삭제 실패: ' + e.message);
            }
        });

        // ====== 편집 모달 ======
        function openEditModal(row){
            editId.value = row.id;
            // 이름/카테고리/전공/위치 값 지정 (리스트에 있으면 선택, 없으면 직접 쓰기)
            function setSelectOrCustom(sel, custom, val){
                sel.value = ''; custom.value = ''; custom.style.display='none';
                if(!val){ sel.value=''; return; }
                // 옵션 중에 있으면 선택
                const has = [...sel.options].some(o=>o.value===val);
                if(has){ sel.value = val; }
                else { sel.value='__CUSTOM__'; custom.style.display='inline-block'; custom.value = val; }
            }
            setSelectOrCustom(editNameSel,  editNameCustom,  row.name||'');
            setSelectOrCustom(editCatSel,   editCatCustom,   row.category||'');
            setSelectOrCustom(editDeptSel,  editDeptCustom,  row.department||'');
            setSelectOrCustom(editLocSel,   editLocCustom,   row.location||'');

            setMsg(editMsg,'');
            editBack.style.display = 'flex';
        }
        function closeEditModal(){
            editBack.style.display = 'none';
        }
        btnEditCancel.addEventListener('click', closeEditModal);
        editBack.addEventListener('click', e=>{ if(e.target===editBack) closeEditModal(); });

        bodyEl.addEventListener('click', e=>{
            if(!e.target.classList.contains('editRow')) return;
            const btn = e.target;
            const row = {
                id: parseInt(btn.dataset.id,10),
                name: btn.dataset.name || '',
                category: btn.dataset.category || '',
                department: btn.dataset.dept || '',
                location: btn.dataset.location || ''
            };
            openEditModal(row);
        });

        editForm.addEventListener('submit', async (e)=>{
            e.preventDefault();
            const id = editId.value;
            const name       = readValue(editNameSel,  editNameCustom);
            const category   = readValue(editCatSel,   editCatCustom);
            const department = readValue(editDeptSel,  editDeptCustom);
            const location   = readValue(editLocSel,   editLocCustom);

            if(!name || !category){ setMsg(editMsg, '이름과 카테고리는 필수입니다.', true); return; }

            const body = new URLSearchParams({ id, name, category, department, location }).toString();
            try{
                await fetchJson(CP + '/admin/tool/update', {
                    method:'POST',
                    headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
                    body
                });
                closeEditModal();
                await loadNames(); await loadList();
            }catch(err){
                setMsg(editMsg, '수정 실패: ' + err.message, true);
            }
        });

        // 최초 로드
        (async function init(){
            await loadMeta();
            await loadNames();
            await loadList();
        })();
    })();
</script>
</body>
</html>
