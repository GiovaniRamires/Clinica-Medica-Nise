const API = 'http://localhost:8080';

// ── State ──
let medicos   = [];
let pacientes = [];
let consultas = []; // local — persiste em localStorage; futuramente endpoint /consultas
let selectedDay  = null;
let selectedSlot = null;

// ── Horários padrão da clínica ──
const HORARIOS = [
  '08:00','08:30','09:00','09:30','10:00','10:30',
  '11:00','11:30','13:00','13:30','14:00','14:30',
  '15:00','15:30','16:00','16:30','17:00','17:30',
];

// ─────────────────────────────────────────────
// NAVIGATION
// ─────────────────────────────────────────────
const PAGE_TITLES = {
  'dashboard':    'Dashboard',
  'agendamentos': 'Agendamentos',
  'nova-consulta':'Nova Consulta',
  'medicos':      'Médicos',
  'pacientes':    'Pacientes',
};

function navigate(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  const page = document.getElementById('page-' + id);
  if (page) page.classList.add('active');
  const btn = [...document.querySelectorAll('.nav-item')]
    .find(b => b.getAttribute('onclick') === `navigate('${id}')`);
  if (btn) btn.classList.add('active');
  document.getElementById('page-title').textContent = PAGE_TITLES[id] || id;

  if (id === 'nova-consulta') { populateSelects(); renderTimeSlots(); }
  if (id === 'agendamentos')  { buildCalStrip(); renderAgenda(); }
  if (id === 'dashboard')     { renderDashboard(); }
}

// ─────────────────────────────────────────────
// API HELPERS
// ─────────────────────────────────────────────
async function apiFetch(path, opts = {}) {
  try {
    const res = await fetch(API + path, {
      headers: { 'Content-Type': 'application/json' },
      ...opts,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    if (res.status === 204) return null;
    return res.json();
  } catch (e) {
    toast(
      '⚠️ ' + (e.message.includes('fetch') ? 'Backend offline — usando dados locais' : e.message),
      'warn'
    );
    return null;
  }
}

// ─────────────────────────────────────────────
// SYNC
// ─────────────────────────────────────────────
async function syncAll() {
  const [m, p, c] = await Promise.all([
    apiFetch('/medicos'),
    apiFetch('/pacientes'),
    apiFetch('/consultas'),
  ]);
  if (m) { medicos   = m; renderMedicos(); }
  if (p) { pacientes = p; renderPacientes(); }
  if (c) {
    // normaliza campos vindos do backend para o formato interno do frontend
    consultas = c.map(normalizeConsulta);
    saveLocal();
    buildCalStrip();
    renderAgenda();
  }
  renderDashboard();
  populateSelects();
  toast('✅ Dados sincronizados!');
}

// Mapeia um objeto de consulta retornado pelo backend para o shape interno.
// Campos esperados do backend (ajuste conforme seu DTO):
//   id, medicoId, pacienteId, medicoNome, pacienteNome, especialidade,
//   data (YYYY-MM-DD), horario (HH:mm), motivo, obs, status, criadaEm
function normalizeConsulta(c) {
  return {
    id:           c.id,
    medicoId:     c.medicoId   ?? c.medico?.id,
    pacienteId:   c.pacienteId ?? c.paciente?.id,
    medicoNome:   c.medicoNome   ?? c.medico?.nome   ?? '—',
    pacienteNome: c.pacienteNome ?? c.paciente?.nome ?? '—',
    especialidade: c.especialidade ?? c.medico?.especialidade ?? '—',
    data:     c.data,
    horario:  c.horario,
    motivo:   c.motivo  ?? '',
    obs:      c.obs     ?? '',
    status:   c.status  ?? 'Agendada',
    criadaEm: c.criadaEm ?? new Date().toISOString(),
  };
}

// ─────────────────────────────────────────────
// MÉDICOS
// ─────────────────────────────────────────────
function renderMedicos() {
  const q = (document.getElementById('search-medico')?.value || '').toLowerCase();
  const list = medicos.filter(m =>
    m.nome.toLowerCase().includes(q) ||
    m.crm.toLowerCase().includes(q) ||
    m.especialidade.toLowerCase().includes(q)
  );
  const tbody = document.getElementById('tbody-medicos');
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state">
      <div class="icon">👨‍⚕️</div>Nenhum médico encontrado.
      <button class="btn btn-primary btn-sm" onclick="openModal('modal-medico')">Cadastrar agora</button>
    </div></td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(m => {
    const qtd = consultas.filter(c => c.medicoId == m.id).length;
    return `<tr>
      <td><div class="name-cell"><div class="avatar">${initials(m.nome)}</div>${m.nome}</div></td>
      <td>${m.crm}</td>
      <td><span class="pill pill-blue">${m.especialidade}</span></td>
      <td>${qtd}</td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="editMedico(${m.id})">✏️</button>
        <button class="btn btn-danger btn-sm"    onclick="deletarMedico(${m.id})">🗑️</button>
      </td>
    </tr>`;
  }).join('');
}

function openModal(id)  { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

function editMedico(id) {
  const m = medicos.find(x => x.id == id);
  if (!m) return;
  document.getElementById('medico-edit-id').value    = m.id;
  document.getElementById('m-nome').value            = m.nome;
  document.getElementById('m-crm').value             = m.crm;
  document.getElementById('m-especialidade').value   = m.especialidade;
  document.getElementById('modal-medico-title').textContent = '✏️ Editar Médico';
  openModal('modal-medico');
}

async function salvarMedico() {
  const id  = document.getElementById('medico-edit-id').value;
  const nome = document.getElementById('m-nome').value.trim();
  const crm  = document.getElementById('m-crm').value.trim();
  const esp  = document.getElementById('m-especialidade').value.trim();
  if (!nome || !crm || !esp) { toast('⚠️ Preencha todos os campos obrigatórios', 'warn'); return; }

  const body = JSON.stringify({ nome, crm, especialidade: esp });
  let result;
  if (id) {
    result = await apiFetch(`/medicos/${id}`, { method: 'PUT', body });
    medicos = medicos.map(m =>
      m.id == id ? (result || { ...m, nome, crm, especialidade: esp }) : m
    );
    toast('✅ Médico atualizado!');
  } else {
    result = await apiFetch('/medicos', { method: 'POST', body });
    medicos.push(result || { id: Date.now(), nome, crm, especialidade: esp });
    toast(result ? '✅ Médico cadastrado!' : '💾 Salvo localmente (backend offline)');
  }
  closeModal('modal-medico');
  clearMedicoForm();
  renderMedicos();
  populateSelects();
  renderDashboard();
}

async function deletarMedico(id) {
  if (!confirm('Excluir este médico?')) return;
  await apiFetch(`/medicos/${id}`, { method: 'DELETE' });
  medicos = medicos.filter(m => m.id != id);
  renderMedicos(); renderDashboard(); populateSelects();
  toast('🗑️ Médico removido');
}

function clearMedicoForm() {
  ['medico-edit-id','m-nome','m-crm','m-especialidade'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('modal-medico-title').textContent = '👨‍⚕️ Cadastrar Médico';
}

// ─────────────────────────────────────────────
// PACIENTES
// ─────────────────────────────────────────────
function renderPacientes() {
  const q = (document.getElementById('search-paciente-list')?.value || '').toLowerCase();
  const list = pacientes.filter(p =>
    p.nome.toLowerCase().includes(q) ||
    (p.cpf || '').toLowerCase().includes(q) ||
    (p.convenio || '').toLowerCase().includes(q)
  );
  const tbody = document.getElementById('tbody-pacientes');
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state">
      <div class="icon">🧑</div>Nenhum paciente encontrado.
      <button class="btn btn-primary btn-sm" onclick="openModal('modal-paciente')">Cadastrar agora</button>
    </div></td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(p => {
    const qtd = consultas.filter(c => c.pacienteId == p.id).length;
    return `<tr>
      <td><div class="name-cell"><div class="avatar">${initials(p.nome)}</div>${p.nome}</div></td>
      <td>${p.cpf || '—'}</td>
      <td>${p.convenio || '<span style="opacity:.5">Particular</span>'}</td>
      <td>${p.alergias
        ? `<span class="pill pill-amber">${p.alergias}</span>`
        : '<span style="opacity:.4">—</span>'}</td>
      <td>${qtd}</td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="editPaciente(${p.id})">✏️</button>
        <button class="btn btn-danger btn-sm"    onclick="deletarPaciente(${p.id})">🗑️</button>
      </td>
    </tr>`;
  }).join('');
}

function editPaciente(id) {
  const p = pacientes.find(x => x.id == id);
  if (!p) return;
  document.getElementById('paciente-edit-id').value  = p.id;
  document.getElementById('p-nome').value            = p.nome;
  document.getElementById('p-cpf').value             = p.cpf || '';
  document.getElementById('p-convenio').value        = p.convenio || '';
  document.getElementById('p-alergias').value        = p.alergias || '';
  document.getElementById('p-prontuario').value      = p.prontuario || '';
  document.getElementById('modal-paciente-title').textContent = '✏️ Editar Paciente';
  openModal('modal-paciente');
}

async function salvarPaciente() {
  const id         = document.getElementById('paciente-edit-id').value;
  const nome       = document.getElementById('p-nome').value.trim();
  const cpf        = document.getElementById('p-cpf').value.trim();
  const convenio   = document.getElementById('p-convenio').value.trim();
  const alergias   = document.getElementById('p-alergias').value.trim();
  const prontuario = document.getElementById('p-prontuario').value.trim();
  if (!nome || !cpf) { toast('⚠️ Nome e CPF são obrigatórios', 'warn'); return; }

  const body = JSON.stringify({ nome, cpf, convenio, alergias, prontuario });
  let result;
  if (id) {
    result = await apiFetch(`/pacientes/${id}`, { method: 'PUT', body });
    pacientes = pacientes.map(p =>
      p.id == id ? (result || { ...p, nome, cpf, convenio, alergias, prontuario }) : p
    );
    toast('✅ Paciente atualizado!');
  } else {
    result = await apiFetch('/pacientes', { method: 'POST', body });
    pacientes.push(result || { id: Date.now(), nome, cpf, convenio, alergias, prontuario });
    toast('✅ Paciente cadastrado!');
  }
  closeModal('modal-paciente');
  clearPacienteForm();
  renderPacientes(); renderDashboard(); populateSelects();
  saveLocal();
}

async function deletarPaciente(id) {
  if (!confirm('Excluir este paciente?')) return;
  await apiFetch(`/pacientes/${id}`, { method: 'DELETE' });
  pacientes = pacientes.filter(p => p.id != id);
  renderPacientes(); renderDashboard(); populateSelects();
  toast('🗑️ Paciente removido');
}

function clearPacienteForm() {
  ['paciente-edit-id','p-nome','p-cpf','p-convenio','p-alergias','p-prontuario'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('modal-paciente-title').textContent = '🧑 Cadastrar Paciente';
}

// ─────────────────────────────────────────────
// NOVA CONSULTA
// ─────────────────────────────────────────────
function populateSelects() {
  const selM = document.getElementById('sel-medico');
  const selP = document.getElementById('sel-paciente');
  if (!selM || !selP) return;
  const prevM = selM.value, prevP = selP.value;
  selM.innerHTML = '<option value="">Selecione um médico</option>' +
    medicos.map(m => `<option value="${m.id}">${m.nome} — ${m.especialidade}</option>`).join('');
  selP.innerHTML = '<option value="">Selecione um paciente</option>' +
    pacientes.map(p => `<option value="${p.id}">${p.nome} (${p.cpf || 'sem CPF'})</option>`).join('');
  if (prevM) selM.value = prevM;
  if (prevP) selP.value = prevP;
}

function onMedicoChange() {
  const id = document.getElementById('sel-medico').value;
  const m  = medicos.find(x => x.id == id);
  document.getElementById('inp-especialidade').value = m ? m.especialidade : '';
  updateResumo();
  renderTimeSlots();
}

function renderTimeSlots() {
  const medicoId  = document.getElementById('sel-medico')?.value;
  const data      = document.getElementById('inp-data')?.value;
  const grid      = document.getElementById('time-grid');
  const placeholder = document.getElementById('slot-placeholder');
  if (!medicoId || !data) {
    grid.style.display = 'none';
    placeholder.style.display = '';
    return;
  }
  grid.style.display = 'grid';
  placeholder.style.display = 'none';

  const ocupados = consultas
    .filter(c => c.medicoId == medicoId && c.data === data && c.status !== 'Cancelada')
    .map(c => c.horario);

  grid.innerHTML = HORARIOS.map(h => {
    const taken = ocupados.includes(h);
    const sel   = selectedSlot === h ? 'selected' : '';
    return `<div class="time-slot ${taken ? 'taken' : sel}"
      onclick="${taken ? '' : `selectSlot('${h}')`}">${h}</div>`;
  }).join('');
}

function selectSlot(h) {
  selectedSlot = h;
  document.getElementById('inp-horario').value        = h;
  document.getElementById('inp-horario-display').value = h;
  renderTimeSlots();
  updateResumo();
}

function updateResumo() {
  const mId    = document.getElementById('sel-medico')?.value;
  const pId    = document.getElementById('sel-paciente')?.value;
  const data   = document.getElementById('inp-data')?.value;
  const hora   = document.getElementById('inp-horario')?.value;
  const motivo = document.getElementById('inp-motivo')?.value;
  const m = medicos.find(x => x.id == mId);
  const p = pacientes.find(x => x.id == pId);
  const r = document.getElementById('resumo-consulta');
  r.innerHTML = [
    p      ? `<b>Paciente:</b> ${p.nome}`          : null,
    m      ? `<b>Médico:</b> ${m.nome}`             : null,
    m      ? `<b>Especialidade:</b> ${m.especialidade}` : null,
    data   ? `<b>Data:</b> ${fmtDate(data)}`        : null,
    hora   ? `<b>Horário:</b> ${hora}`              : null,
    motivo ? `<b>Motivo:</b> ${motivo}`             : null,
  ].filter(Boolean).join('<br>') || 'Preencha os campos ao lado.';
  r.style.color = (p && m && data && hora) ? 'var(--text)' : 'var(--muted)';
}

async function agendarConsulta() {
  const mId    = document.getElementById('sel-medico').value;
  const pId    = document.getElementById('sel-paciente').value;
  const data   = document.getElementById('inp-data').value;
  const hora   = document.getElementById('inp-horario').value;
  const motivo = document.getElementById('inp-motivo').value.trim();
  const obs    = document.getElementById('inp-obs').value.trim();
  if (!mId || !pId || !data || !hora || !motivo) {
    toast('⚠️ Preencha todos os campos obrigatórios', 'warn');
    return;
  }
  const m = medicos.find(x => x.id == mId);
  const p = pacientes.find(x => x.id == pId);

  // Payload enviado ao backend — ajuste os nomes de campos ao seu DTO Java
  const payload = {
    medicoId:   Number(mId),
    pacienteId: Number(pId),
    data,
    horario: hora,
    motivo,
    obs,
    status: 'Agendada',
  };

  const result = await apiFetch('/consultas', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  const novaConsulta = normalizeConsulta(result || {
    id: Date.now(),
    ...payload,
    medicoNome:   m.nome,
    especialidade: m.especialidade,
    pacienteNome: p.nome,
    criadaEm: new Date().toISOString(),
  });

  consultas.push(novaConsulta);
  saveLocal();
  toast(result ? '✅ Consulta agendada!' : '💾 Agendada localmente (backend offline)');
  limparForm();
  renderDashboard();
  navigate('agendamentos');
}

function limparForm() {
  ['sel-medico','sel-paciente','inp-data','inp-horario','inp-horario-display',
   'inp-motivo','inp-obs','inp-especialidade'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  selectedSlot = null;
  renderTimeSlots();
  updateResumo();
}

// ─────────────────────────────────────────────
// AGENDA / CALENDAR
// ─────────────────────────────────────────────
function buildCalStrip() {
  const strip = document.getElementById('cal-strip');
  const days  = ['Dom','Seg','Ter','Qua','Qui','Sex','Sáb'];
  const today = new Date();
  let html = '';
  for (let i = -2; i <= 14; i++) {
    const d   = new Date(today);
    d.setDate(today.getDate() + i);
    const iso = d.toISOString().slice(0,10);
    const hasC  = consultas.some(c => c.data === iso);
    const isSel = selectedDay === iso || (!selectedDay && i === 0);
    html += `<div class="cal-day ${isSel ? 'selected' : ''}" onclick="selectDay('${iso}')">
      <div class="wd">${days[d.getDay()]}</div>
      <div class="dn">${d.getDate()}</div>
      <div class="dot-row">${hasC ? '<div class="cal-dot"></div>' : ''}</div>
    </div>`;
  }
  strip.innerHTML = html;
  if (!selectedDay) selectedDay = today.toISOString().slice(0,10);
}

function selectDay(iso) {
  selectedDay = iso;
  buildCalStrip();
  renderAgenda();
}

function renderAgenda() {
  const q  = (document.getElementById('search-agenda')?.value || '').toLowerCase();
  const st = document.getElementById('filter-status')?.value || '';
  const list = consultas.filter(c =>
    (c.pacienteNome.toLowerCase().includes(q) || c.medicoNome.toLowerCase().includes(q)) &&
    (!st || c.status === st)
  ).sort((a,b) => (a.data + a.horario).localeCompare(b.data + b.horario));

  const tbody = document.getElementById('tbody-agenda');
  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state">
      <div class="icon">📅</div>Nenhuma consulta encontrada
    </div></td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(c => `
    <tr>
      <td><div class="name-cell"><div class="avatar">${initials(c.pacienteNome)}</div>${c.pacienteNome}</div></td>
      <td>${c.medicoNome}</td>
      <td><span class="pill pill-blue">${c.especialidade}</span></td>
      <td>${fmtDate(c.data)} ${c.horario}</td>
      <td>${c.motivo}</td>
      <td>${statusPill(c.status)}</td>
      <td style="display:flex;gap:.3rem;flex-wrap:wrap">
        <button class="btn btn-secondary btn-sm" onclick="verConsulta(${c.id})">👁️</button>
        ${c.status === 'Agendada'
          ? `<button class="btn btn-success btn-sm" onclick="changeStatus(${c.id},'Confirmada')">✔</button>`
          : ''}
        ${c.status !== 'Cancelada' && c.status !== 'Realizada'
          ? `<button class="btn btn-danger btn-sm" onclick="changeStatus(${c.id},'Cancelada')">✖</button>`
          : ''}
        ${c.status === 'Confirmada'
          ? `<button class="btn btn-success btn-sm" onclick="changeStatus(${c.id},'Realizada')">🏁</button>`
          : ''}
        ${c.status === 'Cancelada'
          ? `<button class="btn btn-danger btn-sm" onclick="deletarConsulta(${c.id})" title="Excluir">🗑️</button>`
          : ''}
      </td>
    </tr>`).join('');
}

async function changeStatus(id, status) {
  const c = consultas.find(x => x.id == id);
  if (!c) return;

  // Tenta atualizar no backend via PATCH /consultas/{id}/status
  // Se o backend não suportar PATCH, troca para PUT /consultas/{id}
  const result = await apiFetch(`/consultas/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }) ?? await apiFetch(`/consultas/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ ...c, status }),
  });

  c.status = status;
  saveLocal();
  renderAgenda();
  renderDashboard();
  toast(`Status → ${status}`);
}

async function deletarConsulta(id) {
  if (!confirm('Excluir esta consulta permanentemente?')) return;
  await apiFetch(`/consultas/${id}`, { method: 'DELETE' });
  consultas = consultas.filter(c => c.id != id);
  saveLocal();
  renderAgenda();
  renderDashboard();
  buildCalStrip();
  toast('🗑️ Consulta excluída');
}

function verConsulta(id) {
  const c = consultas.find(x => x.id == id);
  if (!c) return;
  document.getElementById('consulta-detalhe-body').innerHTML = `
    <b>Paciente:</b> ${c.pacienteNome}<br>
    <b>Médico:</b> ${c.medicoNome}<br>
    <b>Especialidade:</b> ${c.especialidade}<br>
    <b>Data:</b> ${fmtDate(c.data)} às ${c.horario}<br>
    <b>Motivo:</b> ${c.motivo}<br>
    ${c.obs ? `<b>Observações:</b> ${c.obs}<br>` : ''}
    <b>Status:</b> ${statusPill(c.status)}<br>
    <b>Agendado em:</b> ${new Date(c.criadaEm).toLocaleString('pt-BR')}
  `;
  openModal('modal-consulta-detalhe');
}

// ─────────────────────────────────────────────
// DASHBOARD
// ─────────────────────────────────────────────
function renderDashboard() {
  const today  = new Date().toISOString().slice(0,10);
  const em7    = new Date(); em7.setDate(em7.getDate() + 7);
  const em7iso = em7.toISOString().slice(0,10);

  const hoje   = consultas.filter(c => c.data === today && c.status !== 'Cancelada').length;
  const semana = consultas.filter(c => c.data > today && c.data <= em7iso && c.status !== 'Cancelada').length;

  document.getElementById('stat-hoje').textContent     = hoje;
  document.getElementById('stat-hoje-sub').textContent = hoje === 1 ? '1 consulta' : `${hoje} consultas`;
  document.getElementById('stat-medicos').textContent  = medicos.length;
  document.getElementById('stat-pacientes').textContent = pacientes.length;
  document.getElementById('stat-semana').textContent   = semana;

  const proximas = [...consultas]
    .filter(c => c.status !== 'Cancelada' && c.data >= today)
    .sort((a,b) => (a.data + a.horario).localeCompare(b.data + b.horario))
    .slice(0, 8);
  const tbody = document.getElementById('tbody-proximas');
  if (!proximas.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state">
      <div class="icon">📋</div>Nenhuma consulta agendada
    </div></td></tr>`;
  } else {
    tbody.innerHTML = proximas.map(c => `<tr>
      <td><div class="name-cell"><div class="avatar">${initials(c.pacienteNome)}</div>${c.pacienteNome}</div></td>
      <td>${c.medicoNome}</td>
      <td><span class="pill pill-blue">${c.especialidade}</span></td>
      <td>${fmtDate(c.data)} ${c.horario}</td>
      <td>${statusPill(c.status)}</td>
      <td><button class="btn btn-secondary btn-sm" onclick="navigate('agendamentos')">Ver</button></td>
    </tr>`).join('');
  }
}

// ─────────────────────────────────────────────
// UTILS
// ─────────────────────────────────────────────
function toast(msg, type = 'ok') {
  const el = document.getElementById('toast');
  el.textContent   = msg;
  el.style.borderColor = type === 'warn' ? 'var(--amber)' : type === 'err' ? 'var(--red)' : 'var(--green)';
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 3000);
}

function initials(name = '') {
  return name.split(' ').slice(0,2).map(w => w[0]).join('').toUpperCase();
}

function fmtDate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function statusPill(s) {
  const map = {
    'Agendada':   'pill-blue',
    'Confirmada': 'pill-green',
    'Realizada':  'pill-gray',
    'Cancelada':  'pill-red',
  };
  return `<span class="pill ${map[s] || 'pill-gray'}">${s}</span>`;
}

function maskCPF(el) {
  let v = el.value.replace(/\D/g, '').slice(0, 11);
  if      (v.length > 9) v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
  else if (v.length > 6) v = v.replace(/(\d{3})(\d{3})(\d+)/,          '$1.$2.$3');
  else if (v.length > 3) v = v.replace(/(\d{3})(\d+)/,                  '$1.$2');
  el.value = v;
}

// ─────────────────────────────────────────────
// LOCAL STORAGE (consultas)
// ─────────────────────────────────────────────
function saveLocal() {
  try { localStorage.setItem('clinica_consultas', JSON.stringify(consultas)); } catch (_) {}
}
function loadLocal() {
  try {
    const raw = localStorage.getItem('clinica_consultas');
    if (raw) consultas = JSON.parse(raw);
  } catch (_) {}
}

// ─────────────────────────────────────────────
// INIT
// ─────────────────────────────────────────────
(async () => {
  loadLocal();

  // Seed de demonstração — sobrescrito pelo backend quando disponível
  if (!medicos.length) {
    medicos = [
      { id: 1, nome: 'Dr. Carlos Mendes',  crm: 'CRM/SP 123456', especialidade: 'Cardiologia' },
      { id: 2, nome: 'Dra. Ana Ferreira',  crm: 'CRM/RJ 654321', especialidade: 'Clínica Geral' },
      { id: 3, nome: 'Dr. Roberto Lima',   crm: 'CRM/MG 111222', especialidade: 'Neurologia' },
    ];
  }
  if (!pacientes.length) {
    pacientes = [
      { id: 1, nome: 'João da Silva',   cpf: '123.456.789-00', convenio: 'Unimed',     alergias: '',          prontuario: '' },
      { id: 2, nome: 'Maria Oliveira',  cpf: '987.654.321-00', convenio: 'Particular', alergias: 'Penicilina', prontuario: '' },
    ];
  }
  if (!consultas.length) {
    const today    = new Date().toISOString().slice(0, 10);
    const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
    consultas = [
      {
        id: 1, medicoId: 1, pacienteId: 1,
        medicoNome: 'Dr. Carlos Mendes', especialidade: 'Cardiologia', pacienteNome: 'João da Silva',
        data: today, horario: '09:00', motivo: 'Retorno checkup', obs: '',
        status: 'Confirmada', criadaEm: new Date().toISOString(),
      },
      {
        id: 2, medicoId: 2, pacienteId: 2,
        medicoNome: 'Dra. Ana Ferreira', especialidade: 'Clínica Geral', pacienteNome: 'Maria Oliveira',
        data: tomorrow.toISOString().slice(0, 10), horario: '10:30',
        motivo: 'Consulta de rotina', obs: 'Paciente hipertensa',
        status: 'Agendada', criadaEm: new Date().toISOString(),
      },
    ];
    saveLocal();
  }

  renderDashboard();
  renderMedicos();
  renderPacientes();
  populateSelects();
  buildCalStrip();

  // Fechar modais clicando no backdrop
  document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
    backdrop.addEventListener('click', e => {
      if (e.target === backdrop) backdrop.classList.remove('open');
    });
  });

  // Tenta sincronizar com o backend
  await syncAll();
})();
