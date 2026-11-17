package com.example.pc

object PcWebHtml {
    fun buildHtml(): String = """
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>DriveDeck — Веб-панель для ПК</title>
  <style>
    :root {
      --bg: #0b111e;
      --surface: #131c2e;
      --surface2: #1e293b;
      --text: #f1f5f9;
      --muted: #94a3b8;
      --primary: #3b82f6;
      --amber: #f59e0b;
      --emerald: #10b981;
      --coral: #ef4444;
      --border: #334155;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg); color: var(--text); line-height: 1.45;
    }
    header {
      background: var(--surface); border-bottom: 1px solid var(--border);
      padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; gap: 16px;
      position: sticky; top: 0; z-index: 10;
    }
    .brand { font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
    .pill { background: rgba(59,130,246,0.18); color: #93c5fd; padding: 4px 10px; border-radius: 999px; font-size: 12px; }
    .container { max-width: 1200px; margin: 0 auto; padding: 24px; }
    .pin-gate {
      max-width: 380px; margin: 80px auto; background: var(--surface); padding: 28px;
      border-radius: 18px; border: 1px solid var(--border); text-align: center;
    }
    input, select, textarea {
      width: 100%; background: var(--surface2); color: var(--text); border: 1px solid var(--border);
      border-radius: 10px; padding: 10px 12px; font-size: 14px; outline: none;
    }
    input:focus, select:focus { border-color: var(--primary); }
    button {
      background: var(--primary); color: #fff; border: none; border-radius: 10px;
      padding: 10px 16px; font-size: 14px; font-weight: 600; cursor: pointer;
    }
    button.secondary { background: var(--surface2); border: 1px solid var(--border); }
    button.danger { background: rgba(239,68,68,0.18); color: #fca5a5; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .card {
      background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 18px;
    }
    .veh-card { cursor: pointer; transition: border-color .15s; }
    .veh-card.active { border-color: var(--primary); box-shadow: 0 0 0 1px var(--primary); }
    .tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; }
    .tab { background: var(--surface); border: 1px solid var(--border); color: var(--muted); }
    .tab.active { background: var(--primary); color: #fff; border-color: var(--primary); }
    table { width: 100%; border-collapse: collapse; font-size: 14px; }
    th, td { text-align: left; padding: 11px 10px; border-bottom: 1px solid var(--border); }
    th { color: var(--muted); font-weight: 600; font-size: 12px; text-transform: uppercase; }
    .row { display: flex; gap: 12px; align-items: center; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-top: 12px; }
    .badge { padding: 3px 8px; border-radius: 999px; font-size: 12px; font-weight: 600; display: inline-block; }
    .b-fuel { background: rgba(245,158,11,0.18); color: #fcd34d; }
    .b-service { background: rgba(16,185,129,0.18); color: #6ee7b7; }
    .b-expense { background: rgba(99,102,241,0.22); color: #a5b4fc; }
    .b-high { background: rgba(239,68,68,0.2); color: #fca5a5; }
    .b-med { background: rgba(245,158,11,0.18); color: #fcd34d; }
    .b-low { background: rgba(59,130,246,0.18); color: #93c5fd; }
    .hidden { display: none !important; }
    .sil { width: 110px; height: 55px; flex-shrink: 0; }
    .muted { color: var(--muted); font-size: 13px; }
  </style>
</head>
<body>
<header>
  <div class="brand">DriveDeck <span class="pill">ПК-консоль по Wi-Fi</span></div>
  <div class="row">
    <button class="secondary" onclick="loadState()">Обновить</button>
    <button class="secondary" onclick="logoutPin()">Сменить PIN</button>
  </div>
</header>

<div id="pinView" class="pin-gate">
  <h2>Введите PIN-код с экрана телефона</h2>
  <p class="muted">Код отображается в приложении на вкладке «Гараж и ПК».</p>
  <input id="pinInput" type="password" inputmode="numeric" maxlength="6" placeholder="4-значный PIN" />
  <br /><br />
  <button style="width:100%" onclick="submitPin()">Подключиться</button>
  <p id="pinErr" style="color:var(--coral);font-size:13px;"></p>
</div>

<div id="appView" class="container hidden">
  <div id="vehiclesGrid" class="grid"></div>

  <div class="tabs">
    <button class="tab active" onclick="switchTab('tasks', this)">Задачи ТО</button>
    <button class="tab" onclick="switchTab('records', this)">Журнал (Заправки и ТО)</button>
    <button class="tab" onclick="switchTab('vehicles', this)">Добавить / Редактировать авто</button>
  </div>

  <div id="tab-tasks" class="card">
    <div class="row" style="justify-content:space-between;flex-wrap:wrap;">
      <h3 style="margin:0">Ближайшие задачи (Todoist-список)</h3>
    </div>
    <div class="form-grid" style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border);">
      <input id="tTitle" placeholder="Название задачи (напр. Замена масла)" />
      <select id="tVehicle"></select>
      <select id="tTrigger">
        <option value="BOTH">Дата или пробег</option>
        <option value="BY_MILEAGE">Только по пробегу</option>
        <option value="BY_DATE">Только по дате</option>
      </select>
      <input id="tDueKm" type="number" placeholder="На пробеге (км)" />
      <input id="tDueDate" type="date" />
      <select id="tPriority">
        <option value="HIGH">Срочно</option>
        <option value="MEDIUM" selected>Скоро</option>
        <option value="LOW">Планово</option>
      </select>
      <button onclick="saveReminder()">Добавить задачу</button>
    </div>
    <table>
      <thead><tr><th>Статус</th><th>Задача</th><th>Транспорт</th><th>Условие</th><th>Приоритет</th><th></th></tr></thead>
      <tbody id="tasksBody"></tbody>
    </table>
  </div>

  <div id="tab-records" class="card hidden">
    <h3 style="margin:0">Журнал заправок, сервиса и расходов</h3>
    <div class="form-grid" style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid var(--border);">
      <select id="rVehicle"></select>
      <select id="rType">
        <option value="REFUEL">Заправка</option>
        <option value="SERVICE">Сервис / ТО</option>
        <option value="EXPENSE">Расход</option>
      </select>
      <input id="rTitle" placeholder="Название / АЗС / СТО" />
      <input id="rCategory" placeholder="Категория (АИ-95, Масло, Мойка)" />
      <input id="rCost" type="number" step="0.01" placeholder="Сумма" />
      <input id="rOdo" type="number" placeholder="Пробег (км)" />
      <input id="rLiters" type="number" step="0.01" placeholder="Литры (для заправки)" />
      <input id="rNotes" placeholder="Комментарий" />
      <button onclick="saveRecord()">Сохранить запись</button>
    </div>
    <table>
      <thead><tr><th>Дата</th><th>Транспорт</th><th>Тип</th><th>Название</th><th>Пробег</th><th>Сумма</th><th></th></tr></thead>
      <tbody id="recordsBody"></tbody>
    </table>
  </div>

  <div id="tab-vehicles" class="card hidden">
    <h3 style="margin:0">Добавить или обновить транспорт</h3>
    <input type="hidden" id="vEditId" />
    <div class="form-grid">
      <input id="vName" placeholder="Название (напр. Мой Tiguan)" />
      <input id="vBrand" placeholder="Марка" />
      <input id="vModel" placeholder="Модель" />
      <input id="vYear" type="number" value="2022" placeholder="Год" />
      <select id="vClass">
        <option value="SEDAN">Авто: Седан</option>
        <option value="CROSSOVER">Авто: Кроссовер</option>
        <option value="WAGON">Авто: Универсал</option>
        <option value="NAKED">Мото: Нейкед</option>
        <option value="SPORT">Мото: Спорт</option>
        <option value="TOUR_ENDURO">Мото: Турэндуро</option>
        <option value="SCOOTER">Мото: Скутер</option>
        <option value="TOURING">Мото: Туристический</option>
      </select>
      <select id="vStyle">
        <option value="TWO_TONE">Стиль: Аэро (двухцветный)</option>
        <option value="SOLID">Стиль: Монолит (силуэт)</option>
        <option value="OUTLINE">Стиль: Чертёж (контур)</option>
      </select>
      <input id="vColor" type="color" value="#1D63DC" />
      <input id="vOdo" type="number" placeholder="Пробег (км)" />
      <input id="vFuel" value="АИ-95" placeholder="Топливо" />
      <button onclick="saveVehicle()">Сохранить транспорт</button>
    </div>
  </div>
</div>

<script>
  let state = { vehicles: [], records: [], reminders: [] };
  let selectedVehId = 'ALL';

  function getPin() { return sessionStorage.getItem('dd_pin') || ''; }
  function logoutPin() { sessionStorage.removeItem('dd_pin'); location.reload(); }

  async function api(path, opts = {}) {
    const res = await fetch(path, {
      ...opts,
      headers: {
        'Content-Type': 'application/json',
        'X-DriveDeck-Pin': getPin(),
        ...(opts.headers || {})
      }
    });
    if (res.status === 401) {
      document.getElementById('pinView').classList.remove('hidden');
      document.getElementById('appView').classList.add('hidden');
      throw new Error('Неверный PIN-код');
    }
    return res.json();
  }

  async function submitPin() {
    const p = document.getElementById('pinInput').value.trim();
    sessionStorage.setItem('dd_pin', p);
    try {
      await loadState();
      document.getElementById('pinView').classList.add('hidden');
      document.getElementById('appView').classList.remove('hidden');
      document.getElementById('pinErr').textContent = '';
    } catch (e) {
      document.getElementById('pinErr').textContent = e.message;
    }
  }

  function svgSilhouette(cls, style, color) {
    const fill = style === 'OUTLINE' ? 'none' : color;
    const stroke = style === 'OUTLINE' ? color : (style === 'TWO_TONE' ? '#0f172a' : 'none');
    const paths = {
      SEDAN: '<path d="M10,40 L10,32 Q10,27 17,26 L31,24 L46,14 Q51,11 61,11 L79,11 Q86,11 92,16 L99,23 L109,25 Q114,26.5 114,32 L114,43 L10,43 Z" />',
      CROSSOVER: '<path d="M11,38 L11,27 Q11,21 16,19 L24,11 Q28,9 35,9 L75,9 Q82,9 87,13 L97,21 L109,23.5 Q114,25 114,31 L114,41 L11,41 Z" /><line x1="31" y1="7" x2="79" y2="7" stroke-width="2" stroke="currentColor"/>',
      WAGON: '<path d="M10,40 L10,28 Q11,18 17,12.5 Q21,10.5 28,10.5 L77,10.5 Q84,10.5 90,15.5 L98,23 L109,25 Q114,26.5 114,32 L114,43 L10,43 Z" />',
      NAKED: '<path d="M16,21 L37,24 L52,24 Q62,16 77,20 L76,26 L48,26 L47,38 L73,38" /><circle cx="87" cy="23" r="3"/>',
      SPORT: '<path d="M13,15 L38,24 L47,27 Q58,15 77,18 L95,21 L94,30 L73,38 L51,38 Z" />',
      TOUR_ENDURO: '<rect x="14" y="12" width="16" height="11"/><path d="M32,23 L52,24 Q62,15 78,19 L91,20 L105,23.5 L91,25 L75,27 L52,27 Z"/><path d="M81,18 L86,8 L90,11 L88,19 Z"/>',
      SCOOTER: '<path d="M19,24 L44,25 L53,36 L73,36 L78,18 L88,21 L86,38 L49,40 L22,34 Z"/>',
      TOURING: '<rect x="13" y="13" width="14" height="11"/><rect x="16" y="25" width="22" height="11"/><path d="M50,24 Q62,16 79,17 L98,20 L97,33 L75,39 L49,38 Z"/><path d="M81,17 L87,7 L93,11 L91,18.5 Z"/>'
    };
    const isMoto = ['NAKED','SPORT','TOUR_ENDURO','SCOOTER','TOURING'].includes(cls);
    const wheels = isMoto
      ? '<circle cx="28" cy="44" r="9" fill="#0f172a" stroke="#cbd5e1" stroke-width="2"/><circle cx="92" cy="44" r="9" fill="#0f172a" stroke="#cbd5e1" stroke-width="2"/>'
      : '<circle cx="30" cy="44" r="8" fill="#0f172a" stroke="#cbd5e1" stroke-width="2"/><circle cx="92" cy="44" r="8" fill="#0f172a" stroke="#cbd5e1" stroke-width="2"/>';
    return `<svg class="sil" viewBox="0 0 120 60" fill="${'$'}{fill}" stroke="${'$'}{stroke}" stroke-width="2">${'$'}{paths[cls] || paths.SEDAN}${'$'}{wheels}</svg>`;
  }

  async function loadState() {
    state = await api('/api/state');
    render();
  }

  function render() {
    const grid = document.getElementById('vehiclesGrid');
    grid.innerHTML = `<div class="card veh-card ${'$'}{selectedVehId==='ALL'?'active':''}" onclick="selectedVehId='ALL';render()">
      <strong>Весь гараж (${'$'}{state.vehicles.length})</strong><p class="muted">Показать все записи и задачи</p>
    </div>` + state.vehicles.map(v => `
      <div class="card veh-card ${'$'}{selectedVehId===v.id?'active':''}" onclick="selectedVehId='${'$'}{v.id}';render()">
        <div class="row">
          ${'$'}{svgSilhouette(v.vehicleClass, v.iconStyle, v.accentColorHex)}
          <div>
            <strong>${'$'}{v.name}</strong>
            <div class="muted">${'$'}{v.vehicleClassLabel} • ${'$'}{v.odometerKm.toLocaleString('ru-RU')} км</div>
            <button class="secondary" style="margin-top:6px;padding:4px 10px;font-size:12px" onclick="event.stopPropagation();fillVehicleForm('${'$'}{v.id}')">Изменить</button>
          </div>
        </div>
      </div>
    `).join('');

    const opts = state.vehicles.map(v => `<option value="${'$'}{v.id}">${'$'}{v.name}</option>`).join('');
    document.getElementById('tVehicle').innerHTML = opts;
    document.getElementById('rVehicle').innerHTML = opts;

    const tasks = state.reminders.filter(t => selectedVehId==='ALL' || t.vehicleId===selectedVehId);
    const vMap = Object.fromEntries(state.vehicles.map(v => [v.id, v.name]));
    document.getElementById('tasksBody').innerHTML = tasks.map(t => `
      <tr>
        <td><input type="checkbox" style="width:18px;height:18px" ${'$'}{t.completed?'checked':''} onchange="toggleTask('${'$'}{t.id}', this.checked)"/></td>
        <td style="${'$'}{t.completed?'text-decoration:line-through;opacity:.6':''}"><strong>${'$'}{t.title}</strong><div class="muted">${'$'}{t.notes || ''}</div></td>
        <td>${'$'}{vMap[t.vehicleId] || '—'}</td>
        <td>${'$'}{t.dueOdometerKm>0 ? t.dueOdometerKm+' км ' : ''}${'$'}{t.dueDateEpochMs>0 ? new Date(t.dueDateEpochMs).toLocaleDateString('ru-RU') : ''}</td>
        <td><span class="badge ${'$'}{t.priority==='HIGH'?'b-high':(t.priority==='MEDIUM'?'b-med':'b-low')}">${'$'}{t.priority}</span></td>
        <td><button class="danger" onclick="delItem('reminders','${'$'}{t.id}')">Удалить</button></td>
      </tr>
    `).join('');

    const recs = state.records.filter(r => selectedVehId==='ALL' || r.vehicleId===selectedVehId);
    document.getElementById('recordsBody').innerHTML = recs.map(r => `
      <tr>
        <td>${'$'}{new Date(r.eventTimestampMs).toLocaleDateString('ru-RU')}</td>
        <td>${'$'}{vMap[r.vehicleId] || '—'}</td>
        <td><span class="badge ${'$'}{r.recordType==='REFUEL'?'b-fuel':(r.recordType==='SERVICE'?'b-service':'b-expense')}">${'$'}{r.recordType}</span></td>
        <td><strong>${'$'}{r.title}</strong><div class="muted">${'$'}{r.categoryTag}${'$'}{r.liters>0 ? ' • '+r.liters+' л' : ''}</div></td>
        <td>${'$'}{r.odometerKm.toLocaleString('ru-RU')} км</td>
        <td><strong>${'$'}{r.amountCost.toFixed(2)}</strong></td>
        <td><button class="danger" onclick="delItem('records','${'$'}{r.id}')">Удалить</button></td>
      </tr>
    `).join('');
  }

  function switchTab(id, btn) {
    ['tasks','records','vehicles'].forEach(t => document.getElementById('tab-'+t).classList.add('hidden'));
    document.getElementById('tab-'+id).classList.remove('hidden');
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
  }

  function fillVehicleForm(id) {
    const v = state.vehicles.find(x => x.id === id);
    if (!v) return;
    document.getElementById('vEditId').value = v.id;
    document.getElementById('vName').value = v.name;
    document.getElementById('vBrand').value = v.brand;
    document.getElementById('vModel').value = v.model;
    document.getElementById('vYear').value = v.year;
    document.getElementById('vClass').value = v.vehicleClass;
    document.getElementById('vStyle').value = v.iconStyle;
    document.getElementById('vColor').value = v.accentColorHex;
    document.getElementById('vOdo').value = v.odometerKm;
    document.getElementById('vFuel').value = v.fuelType;
    switchTab('vehicles', document.querySelectorAll('.tab')[2]);
  }

  async function saveVehicle() {
    await api('/api/vehicles', {
      method: 'POST',
      body: JSON.stringify({
        id: document.getElementById('vEditId').value,
        name: document.getElementById('vName').value,
        brand: document.getElementById('vBrand').value,
        model: document.getElementById('vModel').value,
        year: Number(document.getElementById('vYear').value || 2022),
        vehicleClass: document.getElementById('vClass').value,
        iconStyle: document.getElementById('vStyle').value,
        accentColorHex: document.getElementById('vColor').value,
        odometerKm: Number(document.getElementById('vOdo').value || 0),
        fuelType: document.getElementById('vFuel').value || 'АИ-95'
      })
    });
    document.getElementById('vEditId').value = '';
    await loadState();
  }

  async function saveRecord() {
    await api('/api/records', {
      method: 'POST',
      body: JSON.stringify({
        vehicleId: document.getElementById('rVehicle').value,
        recordType: document.getElementById('rType').value,
        title: document.getElementById('rTitle').value || 'Запись',
        categoryTag: document.getElementById('rCategory').value || 'Общее',
        amountCost: Number(document.getElementById('rCost').value || 0),
        odometerKm: Number(document.getElementById('rOdo').value || 0),
        liters: Number(document.getElementById('rLiters').value || 0),
        notes: document.getElementById('rNotes').value || ''
      })
    });
    document.getElementById('rTitle').value = '';
    await loadState();
  }

  async function saveReminder() {
    const dateVal = document.getElementById('tDueDate').value;
    await api('/api/reminders', {
      method: 'POST',
      body: JSON.stringify({
        vehicleId: document.getElementById('tVehicle').value,
        title: document.getElementById('tTitle').value || 'Задача',
        triggerType: document.getElementById('tTrigger').value,
        dueOdometerKm: Number(document.getElementById('tDueKm').value || 0),
        dueDateEpochMs: dateVal ? new Date(dateVal).getTime() : 0,
        priority: document.getElementById('tPriority').value
      })
    });
    document.getElementById('tTitle').value = '';
    await loadState();
  }

  async function toggleTask(id, completed) {
    await api('/api/reminders/toggle', { method: 'POST', body: JSON.stringify({ id, completed }) });
    await loadState();
  }

  async function delItem(col, id) {
    await api(`/api/${'$'}{col}?id=${'$'}{encodeURIComponent(id)}`, { method: 'DELETE' });
    await loadState();
  }

  if (getPin()) {
    submitPin().catch(() => {});
  }
</script>
</body>
</html>
""".trimIndent()
}
