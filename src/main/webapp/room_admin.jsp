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
    <meta charset="UTF-8" />
    <title>스튜디오/강의실 관리(관리자)</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        h1 { margin-bottom: 16px; }

        fieldset { border: 1px solid #ddd; border-radius: 4px; padding: 12px; margin-bottom: 16px; }
        label { display: inline-block; min-width: 110px; margin: 6px 0; }
        input[type=text], input[type=number], select { width: 260px; padding: 6px 8px; }
        .inline { display:inline-flex; align-items:center; gap:8px; }
        .custom-input { display:none; }
        button { padding: 8px 14px; margin-top: 10px; cursor:pointer; }

        .row { margin-bottom: 10px; }
        .wrap { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
        .panel { background:#fff; border:1px solid #ddd; border-radius:4px; padding:12px; }

        .toolbar { display:flex; justify-content:space-between; align-items:center; margin: 0 0 8px 0; }
        .toolbar .right { display:flex; gap:8px; }

        .table-wrap { border:1px solid #ddd; border-radius:4px; overflow-y:auto; max-height:420px; }
        table { width:100%; border-collapse:separate; border-spacing:0; }
        th, td { border-bottom:1px solid #eee; padding:8px 10px; text-align:left; background:#fff; }
        thead th{ position:sticky; top:0; background:#f7f7f7; border-bottom:1px solid #ddd; z-index:1; }
        tbody tr:nth-child(even){ background:#fafafa; }

        .muted { color:#777; font-size:12px; }
        .error { color:#b00020; font-size:12px; margin-left:8px; }

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
<h1>스튜디오/강의실 관리 (관리자)</h1>

<div class="wrap">
    <!-- 좌측: 입력 -->
    <section class="panel">
        <h3>호실 추가</h3>
        <fieldset>
            <div class="row">
                <label>캠퍼스</label>
                <span class="inline">
                    <select id="campusSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="campusCustom" class="custom-input" placeholder="직접 입력" />
                </span>
            </div>

            <div class="row">
                <label>건물</label>
                <span class="inline">
                    <select id="buildingSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="buildingCustom" class="custom-input" placeholder="직접 입력" />
                </span>
            </div>

            <div class="row">
                <label>층</label>
                <span class="inline">
                    <select id="floorSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="number" id="floorCustom" class="custom-input" placeholder="숫자 입력" step="1" />
                </span>
            </div>

            <div class="row">
                <label>호실</label>
                <span class="inline">
                    <select id="roomSel" disabled>
                        <option value="">불러오는 중…</option>
                    </select>
                    <input type="text" id="roomCustom" class="custom-input" placeholder="예) 301, L01" />
                </span>
            </div>

            <button id="addBtn" type="button">추가</button>
            <span id="addMsg" class="muted"></span>
            <div class="muted" style="margin-top:6px">
                각 항목은 목록에서 선택하거나, 마지막의 <b>직접 쓰기</b>를 선택해 새 값을 입력할 수 있습니다.
            </div>
        </fieldset>
    </section>

    <!-- 우측: 목록 + 선택삭제/편집 -->
    <section class="panel">
        <div class="toolbar">
            <h3 style="margin:0">호실 목록</h3>
            <div class="right">
                <button id="btnBulkDelete" disabled>선택 삭제</button>
            </div>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th style="width:40px"><input type="checkbox" id="chkAll"></th>
                    <th style="width:8%">ID</th>
                    <th style="width:18%">캠퍼스</th>
                    <th style="width:22%">건물</th>
                    <th style="width:12%">층</th>
                    <th style="width:20%">호실</th>
                    <th style="width:12%">추가일</th>
                    <th style="width:70px">편집</th>
                </tr>
                </thead>
                <tbody id="roomBody">
                <tr><td colspan="8" class="muted">불러오는 중…</td></tr>
                </tbody>
            </table>
        </div>
    </section>
</div>

<!-- 편집 모달 -->
<div class="modal-backdrop" id="editBack">
    <div class="modal">
        <h3>호실 편집</h3>
        <form id="editForm">
            <input type="hidden" id="editId">

            <div class="row">
                <label>캠퍼스</label>
                <span class="inline">
                    <select id="editCampusSel"></select>
                    <input type="text" id="editCampusCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>건물</label>
                <span class="inline">
                    <select id="editBuildingSel"></select>
                    <input type="text" id="editBuildingCustom" class="custom-input" placeholder="직접 입력">
                </span>
            </div>

            <div class="row">
                <label>층</label>
                <span class="inline">
                    <select id="editFloorSel"></select>
                    <input type="number" id="editFloorCustom" class="custom-input" placeholder="숫자 입력" step="1">
                </span>
            </div>

            <div class="row">
                <label>호실</label>
                <span class="inline">
                    <select id="editRoomSel"></select>
                    <input type="text" id="editRoomCustom" class="custom-input" placeholder="예) 301, L01">
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

        // 입력 영역
        const campusSel   = $('#campusSel'),   campusCustom   = $('#campusCustom');
        const buildingSel = $('#buildingSel'), buildingCustom = $('#buildingCustom');
        const floorSel    = $('#floorSel'),    floorCustom    = $('#floorCustom');
        const roomSel     = $('#roomSel'),     roomCustom     = $('#roomCustom');
        const addBtn = $('#addBtn'), addMsg = $('#addMsg');

        // 목록/툴바
        const bodyEl = $('#roomBody');
        const chkAll = $('#chkAll');
        const btnBulkDelete = $('#btnBulkDelete');

        // 편집 모달 요소
        const editBack = $('#editBack');
        const editForm = $('#editForm');
        const editMsg  = $('#editMsg');
        const editId   = $('#editId');

        const editCampusSel   = $('#editCampusSel'),   editCampusCustom   = $('#editCampusCustom');
        const editBuildingSel = $('#editBuildingSel'), editBuildingCustom = $('#editBuildingCustom');
        const editFloorSel    = $('#editFloorSel'),    editFloorCustom    = $('#editFloorCustom');
        const editRoomSel     = $('#editRoomSel'),     editRoomCustom     = $('#editRoomCustom');
        const btnEditCancel = $('#btnEditCancel');

        // ===== 공통 유틸 =====
        function setMsg(elOrText, text, isErr){
            // 오버로드: addMsg만 간단히 쓸 수 있도록
            if (typeof elOrText === 'string') { addMsg.textContent = elOrText; addMsg.className = isErr ? 'error':'muted'; return; }
            elOrText.textContent = text || '';
            elOrText.className = isErr ? 'error' : 'muted';
        }

        function fillSelect(sel, values, placeholder){
            sel.innerHTML = '';
            const opt0 = document.createElement('option');
            opt0.value = '';
            opt0.textContent = placeholder || '선택';
            sel.appendChild(opt0);

            (values || []).forEach(v=>{
                if (v===null || v===undefined || v==='') return;
                const o = document.createElement('option');
                o.value = String(v); o.textContent = String(v);
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
                const on = sel.value === '__CUSTOM__';
                input.style.display = on ? 'inline-block' : 'none';
                if (on) input.focus();
            });
        }

        // 입력/편집 셀렉트에 커스텀 토글 연결
        [ [campusSel,campusCustom], [buildingSel,buildingCustom], [floorSel,floorCustom], [roomSel,roomCustom] ]
            .forEach(([s,i])=>bindCustomToggle(s,i));
        [ [editCampusSel,editCampusCustom], [editBuildingSel,editBuildingCustom], [editFloorSel,editFloorCustom], [editRoomSel,editRoomCustom] ]
            .forEach(([s,i])=>bindCustomToggle(s,i));

        function readValue(sel, input){
            return (sel.value === '__CUSTOM__') ? (input.value||'').trim() : (sel.value||'').trim();
        }

        async function fetchJson(url, opts){
            const r = await fetch(url, Object.assign({ headers:{'Accept':'application/json'}, cache:'no-store' }, opts||{}));
            if(!r.ok) throw new Error(url + ' ' + r.status);
            const ct = r.headers.get('content-type') || '';
            if(ct.indexOf('application/json') === -1){
                const t = await r.text(); throw new Error('Unexpected: ' + t.slice(0,120));
            }
            return r.json();
        }

        // ===== 메타 로드 (all 모드 사용) =====
        async function loadMetaAll(){
            try{
                // 서버는 {campuses:[], buildings:[], floors:[], rooms:[]} 반환
                const meta = await fetchJson(CP + '/admin/room/meta?mode=all');
                fillSelect(campusSel,   meta.campuses || [],  '캠퍼스 선택');
                fillSelect(buildingSel, meta.buildings || [], '건물 선택');
                fillSelect(floorSel,    meta.floors || [],    '층 선택');
                fillSelect(roomSel,     meta.rooms || [],     '호실 선택');

                // 편집 모달도 동일 옵션
                fillSelect(editCampusSel,   meta.campuses || [],  '캠퍼스 선택');
                fillSelect(editBuildingSel, meta.buildings || [], '건물 선택');
                fillSelect(editFloorSel,    meta.floors || [],    '층 선택');
                fillSelect(editRoomSel,     meta.rooms || [],     '호실 선택');
            }catch(e){
                console.error('meta load fail:', e);
                [campusSel,buildingSel,floorSel,roomSel, editCampusSel,editBuildingSel,editFloorSel,editRoomSel]
                    .forEach(s=>fillSelect(s, [], (s===floorSel||s===editFloorSel)?'층 선택':(s===roomSel||s===editRoomSel?'호실 선택':(s===buildingSel||s===editBuildingSel?'건물 선택':'캠퍼스 선택'))));
                setMsg(addMsg, '옵션 불러오기 실패 — 직접 쓰기를 사용하세요.', true);
            }
        }

        // ===== 목록 로드 =====
        async function loadList(){
            try{
                const list = await fetchJson(CP + '/admin/room/list');
                bodyEl.innerHTML = '';
                if(!list || !list.length){
                    bodyEl.innerHTML = '<tr><td colspan="8" class="muted">등록된 호실이 없습니다.</td></tr>';
                    return;
                }
                list.forEach(row=>{
                    const tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td><input type="checkbox" class="rowChk" data-id="'+row.id+'"></td>'+
                        '<td>'+row.id+'</td>'+
                        '<td>'+row.campus+'</td>'+
                        '<td>'+row.building+'</td>'+
                        '<td>'+row.floor+'</td>'+
                        '<td>'+row.roomNo+'</td>'+
                        '<td>'+(row.addedAt||'')+'</td>'+
                        '<td><button class="editRow" '+
                        'data-id="'+row.id+'" '+
                        'data-campus="'+(row.campus||'')+'" '+
                        'data-building="'+(row.building||'')+'" '+
                        'data-floor="'+(row.floor||'')+'" '+
                        'data-room="'+(row.roomNo||'')+'">편집</button></td>';
                    bodyEl.appendChild(tr);
                });
            }catch(e){
                console.error(e);
                bodyEl.innerHTML = '<tr><td colspan="8" class="error">목록을 불러오지 못했습니다.</td></tr>';
            }
        }

        // ===== 추가 =====
        addBtn.addEventListener('click', async ()=>{
            const campus   = readValue(campusSel,   campusCustom);
            const building = readValue(buildingSel, buildingCustom);
            const floorRaw = readValue(floorSel,    floorCustom);
            const roomNo   = readValue(roomSel,     roomCustom);

            if(!campus || !building || !floorRaw || !roomNo){
                setMsg(addMsg, '캠퍼스/건물/층/호실은 모두 필수입니다.', true);
                return;
            }
            const floor = parseInt(floorRaw,10);
            if(isNaN(floor)){ setMsg(addMsg, '층은 숫자로 입력하세요.', true); return; }

            setMsg(addMsg, '추가 중…');
            addBtn.disabled = true;

            const body = new URLSearchParams({ campus, building, floor:String(floor), roomNo }).toString();
            try{
                const res = await fetchJson(CP + '/admin/room/add', {
                    method:'POST',
                    headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
                    body
                });
                if(res && res.result === 'OK'){
                    setMsg(addMsg, '추가 완료 (ID ' + (res.id || '?') + ')');
                    [campusSel, buildingSel, floorSel, roomSel].forEach(s=>{ s.value=''; });
                    [campusCustom, buildingCustom, floorCustom, roomCustom].forEach(i=>{ i.value=''; i.style.display='none'; });
                    await loadMetaAll();
                    await loadList();
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

        // ===== 선택 삭제 =====
        function updateBulkDeleteState(){
            const any = !!bodyEl.querySelector('.rowChk:checked');
            btnBulkDelete.disabled = !any;
            const all = bodyEl.querySelectorAll('.rowChk').length;
            const on  = bodyEl.querySelectorAll('.rowChk:checked').length;
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
            const ids = [].map.call(bodyEl.querySelectorAll('.rowChk:checked'), c=>parseInt(c.dataset.id,10));
            if(!ids.length) return;
            if(!confirm(ids.length + '개 항목을 삭제하시겠습니까?')) return;
            try{
                await fetchJson(CP + '/admin/room/delete', {
                    method:'POST',
                    headers:{'Content-Type':'application/json'},
                    body: JSON.stringify({ ids: ids })
                });
                await loadList();
                updateBulkDeleteState();
            }catch(e){
                alert('삭제 실패: ' + e.message);
            }
        });

        // ===== 편집 모달 =====
        function setSelectOrCustom(sel, custom, val){
            sel.value = ''; custom.value = ''; custom.style.display='none';
            if(!val){ sel.value=''; return; }
            const has = [].some.call(sel.options, o=>o.value===String(val));
            if(has){ sel.value = String(val); }
            else { sel.value='__CUSTOM__'; custom.style.display='inline-block'; custom.value = String(val); }
        }

        function openEditModal(row){
            editId.value = row.id;
            setSelectOrCustom(editCampusSel,   editCampusCustom,   row.campus||'');
            setSelectOrCustom(editBuildingSel, editBuildingCustom, row.building||'');
            setSelectOrCustom(editFloorSel,    editFloorCustom,    row.floor||'');
            setSelectOrCustom(editRoomSel,     editRoomCustom,     row.room||'');
            setMsg(editMsg,'');
            editBack.style.display = 'flex';
        }
        function closeEditModal(){ editBack.style.display = 'none'; }
        btnEditCancel.addEventListener('click', closeEditModal);
        editBack.addEventListener('click', e=>{ if(e.target===editBack) closeEditModal(); });

        bodyEl.addEventListener('click', e=>{
            if(!e.target.classList.contains('editRow')) return;
            const btn = e.target;
            const row = {
                id: parseInt(btn.dataset.id,10),
                campus: btn.dataset.campus || '',
                building: btn.dataset.building || '',
                floor: btn.dataset.floor || '',
                room: btn.dataset.room || ''
            };
            openEditModal(row);
        });

        editForm.addEventListener('submit', async (e)=>{
            e.preventDefault();
            const id       = editId.value;
            const campus   = readValue(editCampusSel,   editCampusCustom);
            const building = readValue(editBuildingSel, editBuildingCustom);
            const floorRaw = readValue(editFloorSel,    editFloorCustom);
            const roomNo   = readValue(editRoomSel,     editRoomCustom);

            if(!campus || !building || !floorRaw || !roomNo){
                setMsg(editMsg, '캠퍼스/건물/층/호실은 모두 필수입니다.', true);
                return;
            }
            const floor = parseInt(floorRaw,10);
            if(isNaN(floor)){ setMsg(editMsg, '층은 숫자로 입력하세요.', true); return; }

            const body = new URLSearchParams({ id, campus, building, floor:String(floor), roomNo }).toString();
            try{
                await fetchJson(CP + '/admin/room/update', {
                    method:'POST',
                    headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8'},
                    body
                });
                closeEditModal();
                await loadMetaAll();
                await loadList();
            }catch(err){
                setMsg(editMsg, '수정 실패: ' + err.message, true);
            }
        });

        // 초기 로드
        (async function init(){
            await loadMetaAll();
            await loadList();
        })();
    })();
</script>
</body>
</html>
