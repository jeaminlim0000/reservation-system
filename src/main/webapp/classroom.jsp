<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>스튜디오 대여 현황</title>
    <style>
        body { font-family: Arial, sans-serif; padding:20px; }
        h1 { margin-bottom:20px; }
        .filter { margin-bottom:20px; }
        .filter label { margin-right:15px; }
        .filter input, .filter select { padding:4px 6px; }
        #schedule { }
        #roomList button { margin:0 8px 8px 0; padding:6px 12px; }
        .roomBtn.selected { background:#ddeeff; border-color:#4287f5; }
        .timeslots { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:12px; }
        .timeslot { padding:8px 12px; border:1px solid #444; border-radius:4px; cursor:pointer; transition:background .2s; }
        .timeslot.booked { color:#999; text-decoration:line-through; background:#f5f5f5; cursor:not-allowed; }
        .timeslot.selected { background:#cce0ff; }
        #actionButtons { margin-bottom:20px; }
        #actionButtons button { padding:8px 16px; margin-right:8px; border:none; border-radius:4px; color:#fff; cursor:pointer; }
        #reserveBtn { background:#3366ff; }
        #reserveBtn:disabled { background:#aaa; cursor:not-allowed; }
        #myReservation { width:260px; padding:10px; border:1px solid #ccc; border-radius:4px; background:#fff; box-shadow:0 2px 6px rgba(0,0,0,0.1); }
        #myReservation h2 { margin-top:0; font-size:1.1em; }
        .res-group { margin-bottom:12px; border:1px solid #ddd; border-radius:4px; }
        .res-group h3 { margin:0; padding:6px 8px; background:#f7f7f7; border-bottom:1px solid #ddd; font-size:0.95em; }
        .res-group ul { list-style:none; padding:6px 8px; margin:0; }
        .res-group li { padding:6px; border:1px solid #888; border-radius:4px; margin-bottom:4px; cursor:pointer; transition:background .2s; }
        .res-group li.selected { background:#eef6ff; }
        #myReservation button { width:100%; padding:6px 0; border:none; color:#fff; background:#cc3333; border-radius:4px; cursor:pointer; }
        #myReservation button:disabled { background:#aaa; cursor:not-allowed; }
        #roomName { margin:8px 0 12px; color:#666; font-weight:400; }
    </style>
</head>
<body>
<h1>스튜디오 대여 현황</h1>

<div class="filter">
    <label>날짜:
        <input type="date" id="dateSel"/>
    </label>
    <label>캠퍼스:
        <select id="campus">
            <option value="">선택</option>
        </select>
    </label>
    <label>건물:
        <select id="building" disabled>
            <option value="">선택</option>
        </select>
    </label>
    <label>층:
        <select id="floor" disabled>
            <option value="">선택</option>
        </select>
    </label>
</div>

<div id="schedule">
    <div id="roomList"></div>
    <p id="roomName">교실을 선택하세요.</p>
    <div class="timeslots" id="timeslotContainer"></div>
    <div id="actionButtons">
        <button id="reserveBtn" disabled>예약</button>

    </div>
</div>

<div id="myReservation">
    <h2>내 예약</h2>
    <div id="reservedContainer"></div>
    <button id="cancelBtn" disabled>취소</button>
</div>

<script>
    function localToday(){
        const d = new Date();
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        return d.toISOString().slice(0,10);
    }
    const today = localToday();

    let current       = { campus:"", building:"", floor:"", room:"" };
    let selectedTime  = null;
    let selectedResLi = null;

    document.addEventListener("DOMContentLoaded", () => {
        const dateSel           = document.getElementById("dateSel");
        const campusSel         = document.getElementById("campus");
        const buildingSel       = document.getElementById("building");
        const floorSel          = document.getElementById("floor");
        const roomList          = document.getElementById("roomList");
        const roomName          = document.getElementById("roomName");
        const slotCont          = document.getElementById("timeslotContainer");
        const reserveBtn        = document.getElementById("reserveBtn");
        const reservedContainer = document.getElementById("reservedContainer");
        const cancelBtn         = document.getElementById("cancelBtn");

        dateSel.value = today;
        dateSel.min   = today;
        dateSel.addEventListener('change', () => {
            if (hasSelection()) loadSchedule();
        });
        dateSel.addEventListener('input', (e)=>{
            if (e.target.value && e.target.value < today) e.target.value = today;
        });

        fetch(CP + "/api/room?mode=campuses")
            .then(r=>r.json())
            .then(arr=>{
                campusSel.innerHTML = '<option value="">선택</option>';
                arr.forEach(c => campusSel.appendChild(new Option(c, c)));
            })
            .catch(console.error);

        campusSel.onchange = () => {
            resetBuildingFloorRoom();
            if (!campusSel.value) return;
            buildingSel.disabled = false;
            fetch(CP + "/api/room?mode=buildings&campus=" + encodeURIComponent(campusSel.value))
                .then(r=>r.json())
                .then(arr=>{
                    buildingSel.innerHTML = '<option value="">선택</option>';
                    arr.forEach(b => buildingSel.appendChild(new Option(b, b)));
                })
                .catch(console.error);
        };

        buildingSel.onchange = () => {
            resetFloorRoom();
            if (!buildingSel.value) return;
            floorSel.disabled = false;
            const qs = "mode=floors&campus=" + encodeURIComponent(campusSel.value)
                + "&building=" + encodeURIComponent(buildingSel.value);
            fetch(CP + "/api/room?" + qs)
                .then(r=>r.json())
                .then(arr=>{
                    floorSel.innerHTML = '<option value="">선택</option>';
                    arr.forEach(f => floorSel.appendChild(new Option(f + "층", f)));
                })
                .catch(console.error);
        };

        floorSel.onchange = () => {
            clearRooms();
            if (!floorSel.value) return;
            const qs = "mode=rooms&campus=" + encodeURIComponent(campusSel.value)
                + "&building=" + encodeURIComponent(buildingSel.value)
                + "&floor=" + encodeURIComponent(floorSel.value);
            fetch(CP + "/api/room?" + qs)
                .then(r=>r.json())
                .then(arr=>{
                    arr.forEach(rm => {
                        const btn = document.createElement("button");
                        btn.className = "roomBtn";
                        btn.textContent = rm + "호";
                        btn.onclick = () => selectRoom(rm, btn);
                        roomList.appendChild(btn);
                    });
                    const first = roomList.querySelector("button");
                    if (first) first.click();
                })
                .catch(console.error);
        };

        function selectRoom(r, btn) {
            roomList.querySelectorAll("button").forEach(x=>x.classList.remove("selected"));
            btn.classList.add("selected");
            roomName.textContent = r + "호";
            current = {
                campus: campusSel.value,
                building: buildingSel.value,
                floor: floorSel.value,
                room: r
            };
            selectedTime = null;
            updateActionButtons();
            loadSchedule();
        }

        function hasSelection() {
            return current.campus && current.building && current.floor && current.room;
        }

        function resetBuildingFloorRoom() {
            buildingSel.disabled = true; buildingSel.innerHTML = '<option value="">선택</option>';
            resetFloorRoom();
            clearSchedule();
            current = { campus: campusSel.value || "", building:"", floor:"", room:"" };
        }
        function resetFloorRoom() {
            floorSel.disabled = true; floorSel.innerHTML = '<option value="">선택</option>';
            clearRooms();
            current = { campus: campusSel.value || "", building: buildingSel.value || "", floor:"", room:"" };
        }
        function clearRooms() {
            roomList.innerHTML = "";
            clearSchedule();
            current.room = "";
        }

        function clearSchedule() {
            slotCont.innerHTML = "";
            roomName.textContent = "교실을 선택하세요.";
            selectedTime = null;
            updateActionButtons();
        }

        function updateActionButtons() {
            reserveBtn.disabled = !selectedTime;
        }

        function loadSchedule() {
            if (!hasSelection()) { clearSchedule(); return; }
            slotCont.innerHTML = "";
            const qs =
                "?campus="   + encodeURIComponent(current.campus) +
                "&building=" + encodeURIComponent(current.building) +
                "&floor="    + encodeURIComponent(current.floor) +
                "&room="     + encodeURIComponent(current.room) +
                "&date="     + encodeURIComponent(dateSel.value);
            fetch(CP + "/api/schedule" + qs)
                .then(r=>r.json())
                .then(booked=>{
                    ["09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00"].forEach(t=>{
                        const slot = document.createElement("div");
                        slot.textContent = t;
                        slot.className   = "timeslot" + (booked.includes(t) ? " booked" : "");
                        slot.onclick     = () => {
                            if (slot.classList.contains("booked")) return;
                            slotCont.querySelectorAll(".timeslot").forEach(x=>x.classList.remove("selected"));
                            slot.classList.add("selected");
                            selectedTime = t;
                            updateActionButtons();
                        };
                        slotCont.appendChild(slot);
                    });
                })
                .catch(console.error);
        }

        function loadMyReservations() {
            reservedContainer.innerHTML = "";
            fetch(CP + "/api/reserve", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded"},
                body: "action=list"
            })
                .then(r=>r.json())
                .then(list=>{
                    list.forEach(item=>{
                        if (item.date < today) return;
                        addReservedItem(item.date, item.building, item.room, item.timeslot, item.campus, item.floor);
                    });
                })
                .catch(console.error);
        }

        function addReservedItem(date, building, room, timeslot, campus, floor) {
            let grp = document.getElementById("group-" + date);
            if (!grp) {
                grp = document.createElement("div");
                grp.className = "res-group";
                grp.id        = "group-" + date;
                const hdr = document.createElement("h3");
                hdr.textContent = date;
                grp.appendChild(hdr);
                const ul = document.createElement("ul");
                grp.appendChild(ul);
                reservedContainer.appendChild(grp);
            }
            const ul = grp.querySelector("ul");
            const li = document.createElement("li");
            li.textContent = building + " • " + room + "호 ▶ " + timeslot;
            li.dataset.campus   = campus;
            li.dataset.building = building;
            li.dataset.floor    = floor;
            li.dataset.room     = room;
            li.dataset.timeslot = timeslot;
            li.dataset.date     = date;
            li.onclick = () => {
                reservedContainer.querySelectorAll("li").forEach(x=>x.classList.remove("selected"));
                li.classList.add("selected");
                selectedResLi = li;
                cancelBtn.disabled = false;
            };
            ul.appendChild(li);
        }

        function isSameSelection(d) {
            return d.campus===current.campus &&
                d.building===current.building &&
                String(d.floor)===String(current.floor) &&
                d.room===current.room &&
                d.date===dateSel.value;
        }

        document.getElementById("reserveBtn").onclick = () => {
            if (!selectedTime) return;
            const body =
                "campus="   + encodeURIComponent(current.campus) +
                "&building=" + encodeURIComponent(current.building) +
                "&floor="    + encodeURIComponent(current.floor) +
                "&room="     + encodeURIComponent(current.room) +
                "&timeslot=" + encodeURIComponent(selectedTime) +
                "&date="     + encodeURIComponent(dateSel.value);

            fetch(CP + "/api/reserve", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded"},
                body
            })
                .then(r=>{ if(!r.ok) throw new Error("예약 실패"); return r.text(); })
                .then(txt=>{
                    if (txt !== "OK") throw new Error("예약 실패");
                    loadSchedule();
                    addReservedItem(dateSel.value, current.building, current.room, selectedTime, current.campus, current.floor);
                    selectedTime = null;
                    updateActionButtons();
                })
                .catch(err=>{ console.error(err); alert(err.message); });
        };

        function doCancel() {
            if(!selectedResLi) return;
            const d = selectedResLi.dataset;
            const body =
                "campus="   + encodeURIComponent(d.campus) +
                "&building=" + encodeURIComponent(d.building) +
                "&floor="    + encodeURIComponent(d.floor) +
                "&room="     + encodeURIComponent(d.room) +
                "&timeslot=" + encodeURIComponent(d.timeslot) +
                "&date="     + encodeURIComponent(d.date);
            fetch(CP + "/api/delete", {
                method: "POST",
                headers: {"Content-Type":"application/x-www-form-urlencoded"},
                body
            })
                .then(r=>{ if(!r.ok) throw new Error("취소 실패"); })
                .then(()=>{
                    const grp = selectedResLi.closest(".res-group");
                    selectedResLi.remove();
                    if (!grp.querySelector("li")) grp.remove();
                    selectedResLi = null;
                    cancelBtn.disabled = true;
                    if (hasSelection() && isSameSelection(d)) loadSchedule(); else clearSchedule();
                })
                .catch(err=>{ console.error(err); alert(err.message); });
        }
        cancelBtn.onclick = doCancel;

        loadMyReservations();
    });
</script>
</body>
</html>
