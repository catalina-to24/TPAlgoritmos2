// Capa de presentacion: solo fetch contra /api/* y render. La logica y los calculos
// viven en el backend Java. Este archivo no hace calculos de negocio.

const $ = (sel) => document.querySelector(sel);
const logHost = $('#log');
const toastHost = $('#toastHost');

function toast(kind, text) {
  const el = document.createElement('div');
  el.className = `toast toast-${kind}`;
  el.textContent = text;
  toastHost.appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

function logLine(text, kind = 'info') {
  const li = document.createElement('li');
  const ts = new Date().toLocaleTimeString();
  const color = kind === 'err' ? 'text-danger' : kind === 'ok' ? 'text-success' : 'text-ink-700';
  li.className = color;
  li.textContent = `[${ts}] ${text}`;
  logHost.prepend(li);
}

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

async function refrescarEstado() {
  try {
    const estado = await api('GET', '/api/estado');
    $('#kpiPendientes').textContent = estado.pendientesCentro;
    $('#kpiCamion').textContent = estado.cantidadCamion;
    $('#kpiTope').textContent = estado.topeCamion ? estado.topeCamion.id : '—';

    const camion = await api('GET', '/api/camion');
    const lista = $('#listaCamion');
    lista.innerHTML = '';
    if (camion.paquetes.length === 0) {
      lista.innerHTML = '<li class="text-sm text-ink-400 italic">Camion vacio.</li>';
    } else {
      camion.paquetes.forEach((p, i) => {
        const li = document.createElement('li');
        li.className = 'chip-item';
        li.innerHTML = `
          <div>
            <span class="font-semibold">${p.id}</span>
            <span class="text-ink-500 ml-2">${p.destino} · ${p.peso} kg</span>
          </div>
          <div class="flex items-center gap-2">
            ${p.urgente ? '<span class="px-2 py-0.5 text-xs rounded-full bg-warning/20 text-warning">urgente</span>' : ''}
            ${i === 0 ? '<span class="px-2 py-0.5 text-xs rounded-full bg-primary/10 text-primary font-semibold">TOPE</span>' : ''}
          </div>`;
        lista.appendChild(li);
      });
    }

    const pill = $('#statusPill');
    const dot = pill.querySelector('span:first-child');
    const txt = $('#statusText');
    if (estado.pendientesCentro > 0 || estado.cantidadCamion > 0) {
      pill.classList.remove('hidden');
      pill.classList.add('flex');
      dot.className = 'h-2 w-2 rounded-full bg-success';
      txt.textContent = 'Inventario cargado';
    } else {
      pill.classList.remove('hidden');
      pill.classList.add('flex');
      dot.className = 'h-2 w-2 rounded-full bg-ink-400';
      txt.textContent = 'Listo';
    }
  } catch (e) {
    toast('err', e.message);
  }
}

$('#btnCargarInventario').addEventListener('click', async () => {
  try {
    const r = await api('POST', '/api/inventario/cargar', { path: 'data/inventario.json' });
    toast('ok', `Inventario: ${r.paquetes} paquetes, ${r.depositos} depositos, ${r.rutas} rutas`);
    logLine(`Inventario cargado desde ${r.path}`, 'ok');
    await refrescarEstado();
  } catch (e) {
    toast('err', e.message);
    logLine(`Error cargando inventario: ${e.message}`, 'err');
  }
});

$('#formPaquete').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const form = ev.target;
  const fd = new FormData(form);
  const body = {
    id: fd.get('id'),
    peso: parseFloat(fd.get('peso')),
    destino: fd.get('destino'),
    contenido: fd.get('contenido') || '',
    urgente: fd.get('urgente') === 'on'
  };
  try {
    const p = await api('POST', '/api/centro/paquetes', body);
    toast('ok', `Alta: ${p.id}`);
    logLine(`Alta manual ${p.id} (${p.destino}, ${p.peso} kg${p.urgente ? ', urgente' : ''})`, 'ok');
    form.reset();
    await refrescarEstado();
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnProcesar').addEventListener('click', async () => {
  try {
    const p = await api('POST', '/api/camion/cargar');
    toast('ok', `Cargado al camion: ${p.id}`);
    logLine(`Centro → camion: ${p.id} (${p.destino})`, 'ok');
    await refrescarEstado();
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnDeshacer').addEventListener('click', async () => {
  try {
    const p = await api('POST', '/api/camion/deshacer');
    toast('info', `Undo: ${p.id}`);
    logLine(`Undo tope camion: ${p.id}`, 'info');
    await refrescarEstado();
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnDescargar').addEventListener('click', async () => {
  try {
    const p = await api('POST', '/api/camion/descargar');
    toast('info', `Descargado: ${p.id}`);
    logLine(`Descarga camion: ${p.id}`, 'info');
    await refrescarEstado();
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnAuditar').addEventListener('click', async () => {
  try {
    const fechaRaw = $('#fechaAuditoria').value;
    const body = fechaRaw ? { fechaReferencia: fechaRaw + ':00' } : {};
    const r = await api('POST', '/api/depositos/auditar', body);
    const cont = $('#resultadoAuditoria');
    cont.innerHTML = `
      <span class="text-ink-500">Referencia:</span>
      <span class="font-mono">${r.fechaReferencia}</span><br>
      <span class="text-ink-500">IDs visitados:</span>
      <span class="font-semibold">[${r.idsVisitados.join(', ')}]</span>`;
    logLine(`Auditoria: ${r.idsVisitados.length} depositos marcados`, 'ok');
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnNivel').addEventListener('click', async () => {
  try {
    const n = parseInt($('#nivel').value, 10);
    const r = await api('GET', `/api/depositos/nivel/${n}`);
    const ul = $('#listaNivel');
    ul.innerHTML = '';
    if (r.depositos.length === 0) {
      ul.innerHTML = '<li class="text-ink-400 italic">Nivel vacio.</li>';
    } else {
      r.depositos.forEach(d => {
        const li = document.createElement('li');
        li.className = 'chip-item';
        li.innerHTML = `<span><span class="font-semibold">${d.id}</span> — ${d.nombre}</span>
                        ${d.visitado ? '<span class="px-2 py-0.5 text-xs rounded-full bg-warning/20 text-warning">visitado</span>' : ''}`;
        ul.appendChild(li);
      });
    }
    logLine(`Nivel ${n}: ${r.depositos.length} depositos`, 'info');
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnDistancia').addEventListener('click', async () => {
  try {
    const o = parseInt($('#origen').value, 10);
    const d = parseInt($('#destino').value, 10);
    const r = await api('GET', `/api/rutas/distancia?origen=${o}&destino=${d}`);
    $('#resultadoDistancia').innerHTML = `
      <span class="text-ink-500">Distancia minima</span>
      <span class="font-semibold">${r.origen} → ${r.destino}:</span>
      <span class="text-primary font-bold text-base">${r.distanciaKm} km</span>`;
    logLine(`Dijkstra ${r.origen}→${r.destino} = ${r.distanciaKm} km`, 'ok');
  } catch (e) {
    toast('err', e.message);
  }
});

$('#btnLimpiarLog').addEventListener('click', () => { logHost.innerHTML = ''; });

refrescarEstado();
