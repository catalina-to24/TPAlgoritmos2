// Capa de presentacion: fetch contra /api/* y render. Toda la logica vive en Java.

const $  = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

const state = {
  depositos: [],
  rutas: [],
  estado: { pendientesCentro: 0, cantidadCamion: 0, topeCamion: null },
  inventarioCargado: false,
};

const PAGE_META = {
  inicio:    { title: 'Inicio',    subtitle: 'Panorama general de la operación' },
  paquetes:  { title: 'Paquetes',  subtitle: 'Registrá y procesá paquetes pendientes' },
  demorados: { title: 'Paquetes Demorados', subtitle: 'Paquetes que llevan más de 30 minutos en espera' },
  camion:    { title: 'Camión',    subtitle: 'Carga actual y operaciones de descarga' },
  depositos: { title: 'Depósitos', subtitle: 'Red de depósitos y auditoría' },
  rutas:     { title: 'Rutas',     subtitle: 'Calculá la ruta más corta entre depósitos' },
  guia:      { title: 'Cómo usar', subtitle: 'Guía paso a paso de la aplicación' },
};

// -------------------- toasts + log --------------------
const toastHost = $('#toastHost');
const logHost = $('#log');

function toast(kind, text) {
  const el = document.createElement('div');
  el.className = `toast toast-${kind}`;
  const iconOk  = '<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>';
  const iconErr = '<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>';
  const iconInfo= '<svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>';
  el.innerHTML = (kind === 'ok' ? iconOk : kind === 'err' ? iconErr : iconInfo) + `<span>${escapeHtml(text)}</span>`;
  toastHost.appendChild(el);
  setTimeout(() => el.style.opacity = '0', 2800);
  setTimeout(() => el.remove(), 3200);
}

function logLine(text, kind = 'info') {
  $('#logEmpty')?.classList.add('hidden');
  const li = document.createElement('li');
  const ts = new Date().toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const dot = kind === 'ok' ? 'bg-success' : kind === 'err' ? 'bg-danger' : 'bg-ink-400';
  li.className = 'flex items-start gap-2.5 text-ink-700';
  li.innerHTML = `
    <span class="h-1.5 w-1.5 rounded-full ${dot} mt-2 flex-shrink-0"></span>
    <span class="text-xs text-ink-400 font-mono flex-shrink-0">${ts}</span>
    <span class="flex-1 min-w-0">${escapeHtml(text)}</span>`;
  logHost.prepend(li);
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

// -------------------- api helper --------------------
async function api(method, path, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const msg = data && data.error ? data.error : `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data;
}

// -------------------- router --------------------
function currentRoute() {
  const h = location.hash.replace(/^#\/?/, '');
  const name = (h || 'inicio').split('/')[0];
  return PAGE_META[name] ? name : 'inicio';
}

function navigate() {
  const route = currentRoute();
  $$('.view').forEach(v => v.classList.toggle('hidden', v.dataset.view !== route));
  $$('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.route === route));
  const meta = PAGE_META[route];
  $('#pageTitle').textContent = meta.title;
  $('#pageSubtitle').textContent = meta.subtitle;
  // per-view fetches
  if (route === 'inicio')    refreshAll();
  if (route === 'paquetes')  { refreshEstado(); refreshPendientes(); }
  if (route === 'demorados') { refreshEstado(); refreshPaquetesDemorados(); }
  if (route === 'camion')    { refreshEstado(); refreshCamion(); }
  if (route === 'depositos') refreshDepositos();
  if (route === 'rutas')     { refreshDepositos().then(refreshRutas); }
}

window.addEventListener('hashchange', navigate);

// -------------------- refreshers --------------------
async function refreshEstado() {
  try {
    state.estado = await api('GET', '/api/estado');
    $('#kpiPendientes').textContent = state.estado.pendientesCentro;
    $('#kpiCamion').textContent     = state.estado.cantidadCamion;
    $('#kpiDepositos').textContent  = state.estado.totalDepositos;
    $('#kpiRutas').textContent      = state.estado.totalRutas;
    const topeEl = $('#kpiTope');
    if (topeEl) topeEl.textContent = state.estado.topeCamion ? `tope: ${state.estado.topeCamion.id}` : 'sin carga';
    state.inventarioCargado = !!state.estado.inventarioCargado;
    updateSidebarStatus();
    updateWelcome();
  } catch (e) { toast('err', e.message); }
}

async function refreshPendientes() {
  try {
    const r = await api('GET', '/api/centro/paquetes');
    renderPendientes(r.paquetes || []);
  } catch (e) { toast('err', e.message); }
}

async function refreshPaquetesDemorados() {
  try {
    const r = await api('GET', '/api/centro/paquetes/demorados');
    renderDemorados(r.paquetes || []);
  } catch (e) { toast('err', e.message); }
}

async function refreshCamion() {
  try {
    const r = await api('GET', '/api/camion');
    renderCamion(r.paquetes || [], r.pesoTotal || 0);
  } catch (e) { toast('err', e.message); }
}

async function refreshDepositos() {
  try {
    const r = await api('GET', '/api/depositos');
    state.depositos = r.depositos || [];
    renderDepositos();
    renderDepositosSelects();
  } catch (e) { toast('err', e.message); }
}

async function refreshRutas() {
  try {
    const r = await api('GET', '/api/rutas');
    state.rutas = r.rutas || [];
    renderRutas();
  } catch (e) { toast('err', e.message); }
}

async function refreshAll() {
  await refreshEstado();
}

function updateSidebarStatus() {
  const dot = $('#sidebarStatusDot');
  const txt = $('#sidebarStatusText');
  if (state.inventarioCargado) {
    dot.className = 'h-2 w-2 rounded-full bg-success animate-pulse';
    txt.textContent = 'Operando';
  } else {
    dot.className = 'h-2 w-2 rounded-full bg-ink-400';
    txt.textContent = 'Sin inventario';
  }
}

function updateWelcome() {
  const card = $('#welcomeCard');
  if (!card) return;
  card.classList.toggle('hidden', state.inventarioCargado);
}

// -------------------- renderers --------------------
function prioridadBadge(p) {
  // El backend ya decide si el paquete es prioritario (campo `prioritario`).
  // Solo diferenciamos el label visual entre urgente y prioritario-por-otro-motivo.
  if (p.urgente)     return '<span class="badge badge-urgent">Urgente</span>';
  if (p.prioritario) return '<span class="badge badge-heavy">Prioritario</span>';
  return '<span class="badge badge-normal">Normal</span>';
}

function renderPendientes(paquetes) {
  const ul = $('#listaPendientes');
  const empty = $('#pendientesEmpty');
  ul.innerHTML = '';
  if (paquetes.length === 0) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');
  paquetes.forEach((p, i) => {
    const li = document.createElement('li');
    li.className = 'list-row';
    li.innerHTML = `
      <div class="flex items-center gap-3 min-w-0">
        <div class="h-8 w-8 rounded-lg bg-ink-100 flex items-center justify-center text-xs font-bold text-ink-600 flex-shrink-0">${i + 1}</div>
        <div class="min-w-0">
          <p class="font-semibold text-ink-900">${escapeHtml(p.id)}</p>
          <p class="text-xs text-ink-500 truncate">${escapeHtml(p.destino)} · ${p.peso} kg${p.contenido ? ' · ' + escapeHtml(p.contenido) : ''}</p>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        ${prioridadBadge(p)}
      </div>`;
    ul.appendChild(li);
  });
}

function renderDemorados(paquetes) {
  const ul = $('#listaDemorados');
  const empty = $('#demoradosEmpty');
  const count = $('#demoradosCount');
  ul.innerHTML = '';
  count.textContent = paquetes.length;
  if (paquetes.length === 0) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');
  paquetes.forEach((p) => {
    const li = document.createElement('li');
    li.className = 'list-row';
    const urgenciaColor = p.minutosIngreso > 60 ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning';
    li.innerHTML = `
      <div class="flex items-center gap-3 min-w-0">
        <div class="h-8 w-8 rounded-lg ${urgenciaColor} flex items-center justify-center text-xs font-bold flex-shrink-0">
          ⏱
        </div>
        <div class="min-w-0">
          <p class="font-semibold text-ink-900">${escapeHtml(p.id)}</p>
          <p class="text-xs text-ink-500 truncate">${escapeHtml(p.destino)} · ${p.peso} kg · ${p.minutosIngreso} min en espera</p>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <span class="badge ${p.minutosIngreso > 60 ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning'}">
          +${p.minutosIngreso} min
        </span>
      </div>`;
    ul.appendChild(li);
  });
}

function renderCamion(paquetes, pesoTotal) {
  const ul = $('#listaCamion');
  const empty = $('#camionEmpty');
  ul.innerHTML = '';
  const total = paquetes.length;
  const totalEl = $('#camionTotal');
  const pesoEl = $('#camionPeso');
  if (totalEl) totalEl.textContent = total;
  if (pesoEl)  pesoEl.textContent  = `${pesoTotal.toFixed(1)} kg`;

  if (total === 0) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');
  paquetes.forEach((p, i) => {
    const li = document.createElement('li');
    li.className = 'list-row';
    li.innerHTML = `
      <div class="flex items-center gap-3 min-w-0">
        <div class="h-8 w-8 rounded-lg ${i === 0 ? 'bg-brand text-white' : 'bg-ink-100 text-ink-500'} flex items-center justify-center text-xs font-bold flex-shrink-0">
          ${i === 0 ? '★' : i + 1}
        </div>
        <div class="min-w-0">
          <p class="font-semibold text-ink-900">${escapeHtml(p.id)}</p>
          <p class="text-xs text-ink-500 truncate">${escapeHtml(p.destino)} · ${p.peso} kg</p>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        ${p.urgente ? '<span class="badge badge-urgent">Urgente</span>' : ''}
        ${i === 0 ? '<span class="badge badge-top">Siguiente a entregar</span>' : ''}
      </div>`;
    ul.appendChild(li);
  });
}

function fechaRelativa(iso) {
  if (!iso) return 'nunca auditado';
  const fecha = new Date(iso);
  const diff = (Date.now() - fecha.getTime()) / (1000 * 60 * 60 * 24);
  if (diff < 1)  return 'hoy';
  if (diff < 2)  return 'ayer';
  if (diff < 30) return `hace ${Math.floor(diff)} días`;
  if (diff < 60) return 'hace 1 mes';
  if (diff < 365)return `hace ${Math.floor(diff / 30)} meses`;
  return `hace ${Math.floor(diff / 365)} año${diff >= 730 ? 's' : ''}`;
}

function renderDepositos() {
  const cont = $('#listaDepositos');
  const empty = $('#depositosEmpty');
  const count = $('#depositosCount');
  cont.innerHTML = '';
  if (state.depositos.length === 0) {
    empty.classList.remove('hidden');
    count.textContent = '';
    return;
  }
  empty.classList.add('hidden');
  count.textContent = `${state.depositos.length} depósito${state.depositos.length === 1 ? '' : 's'}`;
  state.depositos.forEach(d => {
    const row = document.createElement('div');
    row.className = 'list-row';
    const estado = d.visitado
      ? '<span class="badge badge-warning">Requiere atención</span>'
      : '<span class="badge badge-success">Al día</span>';
    row.innerHTML = `
      <div class="flex items-center gap-3 min-w-0">
        <div class="h-10 w-10 rounded-lg bg-ink-100 text-ink-600 flex items-center justify-center font-bold flex-shrink-0">${d.id}</div>
        <div class="min-w-0">
          <p class="font-semibold text-ink-900 truncate">${escapeHtml(d.nombre)}</p>
          <p class="text-xs text-ink-500">Última auditoría: ${fechaRelativa(d.fechaUltimaAuditoria)}</p>
        </div>
      </div>
      <div class="flex-shrink-0">${estado}</div>`;
    cont.appendChild(row);
  });
}

function renderDepositosSelects() {
  const origen  = $('#selectOrigen');
  const destino = $('#selectDestino');
  if (!origen || !destino) return;
  const prevOrigen = origen.value;
  const prevDestino = destino.value;
  origen.innerHTML = destino.innerHTML = '<option value="">— Elegí un depósito —</option>';
  state.depositos.forEach(d => {
    const optO = new Option(`${d.id} · ${d.nombre}`, d.id);
    const optD = new Option(`${d.id} · ${d.nombre}`, d.id);
    origen.appendChild(optO);
    destino.appendChild(optD);
  });
  if (prevOrigen)  origen.value  = prevOrigen;
  if (prevDestino) destino.value = prevDestino;
}

function renderRutas() {
  const ul = $('#listaRutas');
  const empty = $('#rutasEmpty');
  if (!ul) return;
  ul.innerHTML = '';
  if (state.rutas.length === 0) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');
  const nombres = Object.fromEntries(state.depositos.map(d => [d.id, d.nombre]));
  state.rutas.forEach(r => {
    const li = document.createElement('li');
    li.className = 'flex items-center justify-between gap-3 px-3 py-2 rounded-lg bg-ink-50';
    li.innerHTML = `
      <span class="text-ink-700 truncate">${escapeHtml(nombres[r.origen] || r.origen)} ↔ ${escapeHtml(nombres[r.destino] || r.destino)}</span>
      <span class="font-mono font-semibold text-brand flex-shrink-0">${r.distanciaKm} km</span>`;
    ul.appendChild(li);
  });
}

// -------------------- actions --------------------
async function cargarInventario() {
  try {
    const r = await api('POST', '/api/inventario/cargar', { path: 'data/inventario.json' });
    toast('ok', `${r.paquetes} paquetes, ${r.depositos} depósitos, ${r.rutas} rutas`);
    logLine(`Inventario cargado: ${r.paquetes} paquetes, ${r.depositos} depósitos, ${r.rutas} rutas`, 'ok');
    await refreshAll();
    if (currentRoute() === 'paquetes') refreshPendientes();
    if (currentRoute() === 'camion') refreshCamion();
  } catch (e) {
    toast('err', e.message);
    logLine(`Error cargando inventario: ${e.message}`, 'err');
  }
}

function bindActions() {
  $('#btnCargarInventario').addEventListener('click', cargarInventario);
  $('#btnWelcomeCargar')?.addEventListener('click', cargarInventario);

  $('#btnLimpiarLog')?.addEventListener('click', () => {
    logHost.innerHTML = '';
    $('#logEmpty')?.classList.remove('hidden');
  });

  $('#btnQuickProcesar')?.addEventListener('click', async () => {
    try {
      const p = await api('POST', '/api/camion/cargar');
      toast('ok', `Cargado: ${p.id}`);
      logLine(`Enviado al camión: ${p.id} (${p.destino})`, 'ok');
      await refreshEstado();
    } catch (e) { toast('err', e.message); }
  });

  $('#formPaquete')?.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    const body = {
      id: fd.get('id'),
      peso: parseFloat(fd.get('peso')),
      destino: fd.get('destino'),
      contenido: fd.get('contenido') || '',
      urgente: fd.get('urgente') === 'on',
    };
    try {
      const p = await api('POST', '/api/centro/paquetes', body);
      toast('ok', `Paquete ${p.id} registrado`);
      logLine(`Nuevo paquete: ${p.id} (${p.destino}, ${p.peso} kg${p.urgente ? ', urgente' : ''})`, 'ok');
      ev.target.reset();
      await refreshEstado();
      await refreshPendientes();
    } catch (e) { toast('err', e.message); }
  });

  $('#btnProcesar')?.addEventListener('click', async () => {
    try {
      const p = await api('POST', '/api/camion/cargar');
      toast('ok', `Enviado: ${p.id}`);
      logLine(`Enviado al camión: ${p.id} (${p.destino})`, 'ok');
      await refreshEstado();
      await refreshPendientes();
    } catch (e) { toast('err', e.message); }
  });

  $('#btnRefreshDemorados')?.addEventListener('click', async () => {
    try {
      logLine('Actualizando lista de paquetes demorados...', 'info');
      await refreshPaquetesDemorados();
      toast('ok', 'Lista actualizada');
    } catch (e) { toast('err', e.message); }
  });

  $('#btnDeshacer')?.addEventListener('click', async () => {
    try {
      const p = await api('POST', '/api/camion/deshacer');
      toast('info', `Deshecho: ${p.id}`);
      logLine(`Deshecho del camión: ${p.id}`, 'info');
      await refreshEstado();
      await refreshCamion();
    } catch (e) { toast('err', e.message); }
  });

  $('#btnDescargar')?.addEventListener('click', async () => {
    try {
      const p = await api('POST', '/api/camion/descargar');
      toast('info', `Descargado: ${p.id}`);
      logLine(`Descargado del camión: ${p.id}`, 'info');
      await refreshEstado();
      await refreshCamion();
    } catch (e) { toast('err', e.message); }
  });

  $('#btnAuditar')?.addEventListener('click', async () => {
    try {
      const fechaRaw = $('#fechaAuditoria').value;
      const body = fechaRaw ? { fechaReferencia: fechaRaw + ':00' } : {};
      const r = await api('POST', '/api/depositos/auditar', body);
      const res = $('#auditoriaResultado');
      res.classList.remove('hidden');
      res.innerHTML = `
        <div class="flex items-start gap-3 p-4 bg-warning-soft rounded-lg">
          <svg class="h-5 w-5 text-warning flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
          <div class="flex-1">
            <p class="text-sm font-semibold text-ink-900">Auditoría completada</p>
            <p class="text-sm text-ink-700 mt-0.5">
              ${r.idsVisitados.length} depósito${r.idsVisitados.length === 1 ? '' : 's'} necesita${r.idsVisitados.length === 1 ? '' : 'n'} atención:
              <span class="font-mono font-semibold">${r.idsVisitados.join(', ') || '—'}</span>
            </p>
          </div>
        </div>`;
      logLine(`Auditoría: ${r.idsVisitados.length} depósitos marcados`, 'ok');
      await refreshDepositos();
    } catch (e) { toast('err', e.message); }
  });

  $('#btnDistancia')?.addEventListener('click', async () => {
    try {
      const o = $('#selectOrigen').value;
      const d = $('#selectDestino').value;
      if (!o || !d) { toast('err', 'Elegí origen y destino'); return; }
      const r = await api('GET', `/api/rutas/distancia?origen=${o}&destino=${d}`);
      const nombres = Object.fromEntries(state.depositos.map(x => [x.id, x.nombre]));
      $('#resultadoDistancia').innerHTML = `
        <div class="p-5 bg-gradient-to-br from-brand to-accent rounded-xl text-white">
          <p class="text-xs uppercase tracking-wide text-white/80 font-semibold">Ruta más corta</p>
          <p class="text-3xl font-bold mt-1">${r.distanciaKm} km</p>
          <p class="text-sm text-white/90 mt-2">
            ${escapeHtml(nombres[r.origen] || r.origen)} <span class="opacity-60">→</span> ${escapeHtml(nombres[r.destino] || r.destino)}
          </p>
        </div>`;
      logLine(`Ruta ${r.origen}→${r.destino}: ${r.distanciaKm} km`, 'ok');
    } catch (e) { toast('err', e.message); }
  });

  // mobile nav
  $('#btnMobileNav')?.addEventListener('click', () => $('#mobileNav').classList.toggle('hidden'));
  $$('#mobileNav [data-close-nav]').forEach(el => {
    el.addEventListener('click', () => $('#mobileNav').classList.add('hidden'));
  });
}

// -------------------- init --------------------
document.addEventListener('DOMContentLoaded', () => {
  if (!location.hash) location.hash = '#/inicio';
  bindActions();
  navigate();
});
