# Resultado de pruebas manuales — Logi-UADE 2026

Pruebas end-to-end de la UI web ejecutadas sobre **`https://logistica.opendata.com.ar/`** (tunel Cloudflare) con Chromium (Playwright MCP, modo headed sobre RDP `DISPLAY=:10.0`). Los screenshots viven en `tmp/pruebas-ui/`.

- **Entorno**: `java -cp out ar.edu.uade.logistica.Main` (servidor HTTP JDK, puerto 7070) corriendo en una sesion tmux (`logiuade`), expuesto publicamente via `cloudflared` detras de `logistica.opendata.com.ar`.
- **Cache**: el `StaticHandler` envia `Cache-Control: no-store, must-revalidate` para que Cloudflare no cachee HTML/JS/CSS. Las URLs de assets en `index.html` llevan `?v=2` como cache-bust.
- **Dataset**: `data/inventario.json` — 3 paquetes, 6 depositos, 6 rutas.
- **Fecha de ejecucion**: 2026-04-11.
- **UI**: SPA con sidebar, ruteo por hash. 6 vistas (`#/inicio`, `#/paquetes`, `#/camion`, `#/depositos`, `#/rutas`, `#/guia`).

Ver `FLUJOS.md` para la descripcion funcional de cada flujo.

---

## Flujo 1 — Cargar inventario

**Resultado: ✅ PASS**

- **Accion**: click en `Cargar inventario` del header desde la vista Inicio.
- **Estado previo**: sidebar `Sin inventario`, welcome card "Empezá acá" visible, KPIs en 0/0/0/0, log vacio con empty state "Sin actividad todavia".
- **Estado posterior**:
  - KPIs: `Paquetes pendientes = 3`, `En el camion = 0`, `Depositos = 6`, `Rutas = 6`.
  - Sidebar pasa a `Operando` (punto verde pulsante).
  - Welcome card desaparece.
  - Log: `Inventario cargado: 3 paquetes, 6 depositos, 6 rutas`.
  - Toast verde: `3 paquetes, 6 depositos, 6 rutas`.
- **Verificacion**: el servicio reinicia el estado y popula los TDAs (`PriorityQueue`, ABB, grafo) desde el JSON. El flag `inventarioCargado` del `/api/estado` pasa a `true` y el front-end lo usa para esconder el welcome card.
- **Screenshots**: `01a-inicio-vacio.png`, `01b-inicio-cargado.png`.

---

## Flujo 2 — Registrar paquete manual

**Resultado: ✅ PASS**

- **Accion**: navegacion a Paquetes → form con `ID=PKG-TEST`, `Peso=25`, `Destino=Mar del Plata`, `Contenido=Medicamentos`, `Urgente=true`. Click `Registrar paquete`.
- **Estado observado**:
  - La lista `Proximos a cargar` pasa de 3 a 4 items.
  - Orden final (por prioridad + FIFO de llegada):
    1. `PKG-002 Cordoba 72 kg Fragil` — badge `Prioritario` (peso > 50).
    2. `PKG-003 Rosario 8.2 kg Alimentos` — badge `Urgente`.
    3. `PKG-TEST Mar del Plata 25 kg Medicamentos` — badge `Urgente`.
    4. `PKG-001 Buenos Aires 12.5 kg Electronica` — badge `Normal`.
  - Form reseteado (inputs vacios, checkbox desmarcado).
  - Toast `Paquete PKG-TEST registrado`.
- **Verificacion**: el backend construyo el `Paquete<String>` con `prioritario=true` (urgente) y lo inserto en la cola prioritaria con desempate por orden de llegada. El front-end usa el campo `prioritario` del payload para el badge — no replica la regla `peso > 50` en JS.
- **Screenshots**: `02a-paquetes-lista.png` (lista inicial con los 3 paquetes del inventario), `02b-form-lleno.png`, `02c-alta-ok.png`.

---

## Flujo 3 — Enviar siguiente al camion (prioridad)

**Resultado: ✅ PASS**

- **Accion**: click `Enviar siguiente` 4 veces consecutivas.
- **Orden observado** (entrada a la pila en ese orden, confirmado por la pila LIFO y el feed de actividad):
  1. `PKG-002` — Cordoba, 72 kg → sale primero (prioritario por peso, llego primero al centro).
  2. `PKG-003` — Rosario, 8.2 kg, urgente → prioritario por `urgente`.
  3. `PKG-TEST` — Mar del Plata, 25 kg, urgente → prioritario por `urgente`, pero llego despues que PKG-003 (desempate FIFO).
  4. `PKG-001` — Buenos Aires, 12.5 kg → **ultimo**, sin prioridad.
- **Estado final de la vista Paquetes**: lista vacia con empty state `No hay paquetes pendientes. Registra uno nuevo o carga el inventario de ejemplo.`
- **Verificacion**: la `PriorityQueue` saco los 3 prioritarios (peso > 50 o urgente) antes que el normal. Entre prioritarios, orden FIFO por `ordenLlegada` correcto. Complejidad `O(log n)` por operacion.
- **Screenshot**: `03-pendientes-vacio.png`.

---

## Flujo 4 — Ver carga del camion (LIFO)

**Resultado: ✅ PASS**

- **Accion**: navegacion a la vista Camion.
- **Orden observado** (tope arriba, marcado con ★ y chip `Siguiente a entregar`):
  1. `PKG-001` — Buenos Aires, 12.5 kg — **TOPE**.
  2. `PKG-TEST` — Mar del Plata, 25 kg — chip `Urgente`.
  3. `PKG-003` — Rosario, 8.2 kg — chip `Urgente`.
  4. `PKG-002` — Cordoba, 72 kg — sin chip.
- **Tarjetas laterales**:
  - `Total cargado = 4`.
  - `Peso total = 117.7 kg` (= 12.5 + 25 + 8.2 + 72).
- **Verificacion**: LIFO puro — el ultimo cargado (`PKG-001`) quedo en el tope. El peso total lo calcula `Camion.pesoTotal()` en Java y llega al front como campo `pesoTotal` en `/api/camion` — el JS NO lo suma.
- **Screenshot**: `04-camion-lifo.png`.

---

## Flujo 5 — Deshacer ultima carga

**Resultado: ✅ PASS**

- **Accion**: click `Deshacer` con el camion en el estado del flujo 4.
- **Estado observado**:
  - Total: `4 → 3`.
  - Peso total: `117.7 kg → 105.2 kg` (−12.5).
  - Nuevo tope: `PKG-TEST` con chip `Siguiente a entregar`.
  - `PKG-001` desaparecio de la lista.
  - Toast info `Deshecho: PKG-001`.
- **Verificacion**: `pop()` del `ArrayDeque` del camion en `O(1)`. El resto de la pila intacta.
- **Screenshot**: `05-deshacer.png`.

---

## Flujo 6 — Descargar tope del camion

**Resultado: ✅ PASS**

- **Accion**: click `Descargar` con el camion en `[PKG-TEST, PKG-003, PKG-002]`.
- **Estado observado**:
  - Total: `3 → 2`.
  - Peso total: `105.2 kg → 80.2 kg` (−25).
  - Nuevo tope: `PKG-003`.
  - Toast info `Descargado: PKG-TEST`.
- **Verificacion**: misma operacion fisica que `Deshacer` (ambos son `pop()` en `O(1)`). La UI diferencia los toasts y el feed de actividad por tipo (`Descargado del camion:` vs `Deshecho del camion:`) para dejar clara la semantica.
- **Screenshot**: `06-descargar.png`.

---

## Flujo 7 — Auditoria de depositos (post-orden)

**Resultado: ✅ PASS**

- **Accion**: navegacion a Depositos → fijar `Fecha de referencia = 2026-04-10T00:00` → click `Ejecutar auditoria`.
- **Estado previo**: 6 depositos listados en orden **in-order del ABB** (10, 20, 30, 50, 80, 90) con fechas relativas (`nunca auditado`, `hace 22 dias`, `hace 1 mes`, `hace 3 meses`, `hace 4 meses`, `hace 2 meses`). Todos con badge `Al dia`.
- **Estado posterior**:
  - Banner amarillo: `Auditoria completada — 5 depositos necesitan atencion: 10, 30, 90, 80, 50`.
  - IDs en **orden post-orden** correcto.
  - Depositos marcados como `Requiere atencion`: `10 Salta`, `30 Santa Fe`, `50 Hub Central Buenos Aires`, `80 Mendoza`, `90 Neuquen`.
  - Unico `Al dia`: `20 Cordoba` (fecha `2026-03-20`, dentro del cutoff `2026-04-10 − 30 dias = 2026-03-11`).
- **Verificacion** (limite = `2026-03-11`):
  - `50` — `2026-01-10` → anterior → visitado ✓
  - `20` — `2026-03-20` → posterior al limite → **NO visitado** ✓
  - `10` — `null` → visitado ✓
  - `30` — `2026-03-01` → anterior → visitado ✓
  - `80` — `2025-12-01` → anterior → visitado ✓
  - `90` — `2026-02-01` → anterior → visitado ✓
  - Orden post-orden esperado para ABB con raiz 50, izq 20 (→ 10, 30), der 80 (→ 90): `10, 30, [20 no visitado], 90, 80, 50`.
  - Dado que solo se emiten los visitados, la lista resultante es `[10, 30, 90, 80, 50]` ✓.
- **Screenshots**: `07a-depositos-inicial.png`, `07b-auditoria-resultado.png`.

---

## Flujo 8 — Ver red de depositos (listado in-order)

**Resultado: ✅ PASS**

- **Accion**: navegacion a la vista Depositos (flujo que ya quedo cubierto por los screenshots del flujo 7).
- **Endpoint**: `GET /api/depositos`.
- **Backend**: `ArbolDepositos.listarTodos()` → recorrido in-order del ABB.
- **Estado observado**: 6 cards en orden ascendente por ID (`10, 20, 30, 50, 80, 90`) con nombre, fecha relativa de ultima auditoria y badge de estado.
- **Verificacion**: el orden coincide con el in-order del ABB (que para un BST equivale a "ordenado por clave"). Cada deposito muestra el nombre humano y no solo el ID, haciendo la vista autoexplicativa. Los badges coinciden con el post-auditoria del flujo 7.
- **Screenshots**: compartidos con flujo 7 (`07a-depositos-inicial.png`, `07b-auditoria-resultado.png`).

---

## Flujo 9 — Calcular ruta mas corta (Dijkstra)

**Resultado: ✅ PASS**

- **Accion**: navegacion a Rutas → seleccionar `Origen = 10 · Deposito Salta` y `Destino = 90 · Deposito Neuquen` en los dropdowns (poblados con nombre + ID en vez de inputs numericos) → click `Calcular`.
- **Estado observado**:
  - Card gradient con `2050 km` grande y `Deposito Salta → Deposito Neuquen` debajo.
  - Panel lateral `Conexiones disponibles` listo las 6 aristas del grafo con nombres de depositos en vez de IDs crudos:
    - Mendoza ↔ Neuquen = 520 km
    - Hub Central BsAs ↔ Mendoza = 1050 km
    - Cordoba ↔ Hub Central BsAs = 700 km
    - Cordoba ↔ Santa Fe = 330 km
    - Salta ↔ Cordoba = 740 km
    - Santa Fe ↔ Neuquen = 980 km
- **Verificacion**: ruta optima esperada `10 → 20 → 30 → 90 = 740 + 330 + 980 = 2050 km`. Dijkstra con `PriorityQueue` sobre lista de adyacencia, `O((V + E) log V)`. El front-end resuelve los nombres usando el listado que ya tiene en memoria (`state.depositos`), no los pide de nuevo.
- **Screenshot**: `09-rutas-dijkstra.png`.

---

## Flujo 10 — Guia de uso y feed de actividad

**Resultado: ✅ PASS**

- **Accion**: navegacion a `Como usar` (vista Guia) → volver a Inicio y revisar el feed de actividad.
- **Vista Guia**:
  - Card superior gradiente con titulo y bajada.
  - 6 tarjetas numeradas: `Carga el inventario`, `Registra paquetes`, `Envia al camion`, `Deshacer o descargar`, `Audita depositos`, `Calcula rutas`.
  - Bloque `<details>` colapsado `Sobre las estructuras internas` (detalles academicos opt-in).
- **Estado final de los KPIs de Inicio**:
  - `Paquetes pendientes = 0`
  - `En el camion = 2` (tope: `PKG-003`)
  - `Depositos = 6`
  - `Rutas = 6`
- **Feed de actividad** (mas reciente arriba, 10 entradas, timestamps relativos):
  1. `Ruta 10→90: 2050 km`
  2. `Auditoria: 5 depositos marcados`
  3. `Descargado del camion: PKG-TEST`
  4. `Deshecho del camion: PKG-001`
  5. `Enviado al camion: PKG-001 (Buenos Aires)`
  6. `Enviado al camion: PKG-TEST (Mar del Plata)`
  7. `Enviado al camion: PKG-003 (Rosario)`
  8. `Enviado al camion: PKG-002 (Cordoba)`
  9. `Nuevo paquete: PKG-TEST (Mar del Plata, 25 kg, urgente)`
  10. `Inventario cargado: 3 paquetes, 6 depositos, 6 rutas`
- **Verificacion**: cada accion quedo registrada en orden inverso. Los KPIs reflejan exactamente el estado final (post-undo + post-descarga = 2 paquetes en el camion con tope `PKG-003`). Las consultas de solo-lectura (auditoria, rutas) no alteraron los KPIs de inventario/camion.
- **Screenshots**: `10-guia.png`, `11-inicio-final.png`.

---

## Flujo API extra — BFS por nivel (no expuesto en la UI)

**Resultado: ✅ PASS** (testeado via `curl`).

El endpoint `GET /api/depositos/nivel/{n}` existe por requisito de la consigna (BFS sobre el ABB) pero no aparece en la UI de produccion. Verificado por CLI atravesando el tunel de Cloudflare:

```bash
$ curl -s https://logistica.opendata.com.ar/api/depositos/nivel/0
{"depositos":[{"id":50,"nombre":"Hub Central Buenos Aires","visitado":true,"fechaUltimaAuditoria":"2026-01-10T09:00"}],"nivel":0}

$ curl -s https://logistica.opendata.com.ar/api/depositos/nivel/1
{"depositos":[
  {"id":20,"nombre":"Deposito Cordoba","visitado":false,...},
  {"id":80,"nombre":"Deposito Mendoza","visitado":true,...}
],"nivel":1}

$ curl -s https://logistica.opendata.com.ar/api/depositos/nivel/2
{"depositos":[
  {"id":10,"nombre":"Deposito Salta","visitado":true,"fechaUltimaAuditoria":null},
  {"id":30,"nombre":"Deposito Santa Fe","visitado":true,...},
  {"id":90,"nombre":"Deposito Neuquen","visitado":true,...}
],"nivel":2}

$ curl -s https://logistica.opendata.com.ar/api/depositos/nivel/3
{"depositos":[],"nivel":3}
```

**Verificacion**: con raiz 50, hijos 20 y 80, nietos 10, 30, 90 (3 hojas a la derecha del 20, 1 hoja del 80). El BFS devuelve los niveles correctos y el `visitado` es coherente con la auditoria del flujo 7 (solo Cordoba quedo `false`).

---

## Resumen

| # | Flujo                                           | Resultado |
|---|-------------------------------------------------|-----------|
| 1 | Cargar inventario                               | ✅ PASS   |
| 2 | Registrar paquete manual                        | ✅ PASS   |
| 3 | Enviar siguiente al camion (prioridad)          | ✅ PASS   |
| 4 | Ver carga del camion (LIFO)                     | ✅ PASS   |
| 5 | Deshacer ultima carga                           | ✅ PASS   |
| 6 | Descargar tope del camion                       | ✅ PASS   |
| 7 | Auditoria de depositos (post-orden)             | ✅ PASS   |
| 8 | Ver red de depositos (in-order)                 | ✅ PASS   |
| 9 | Calcular ruta mas corta (Dijkstra)              | ✅ PASS   |
| 10 | Guia de uso y feed de actividad                | ✅ PASS   |
| + | BFS por nivel (API no expuesta)                 | ✅ PASS   |

**10/10 flujos de UI en verde + 1 flujo de API**. Ningun bug observado. Unica console warning: `favicon.ico 404` → resuelto agregando un favicon inline SVG en el `<head>`; el warning es del request previo cacheado del browser.

---

## Anexo — Material de estudio en `/cuestionario` (2026-04-23)

Al aproximarse el primer parcial se agrego material de estudio servido por el mismo `WebServer` de Java, reemplazando al viejo `cuestionario.html` single-file. La estructura (hub → quiz → galeria de resumenes) esta inspirada en el repo hermano `../gestion`.

**Verificacion smoke via curl** (puerto 7080 local, el server viejo del usuario seguia corriendo en 7070 como root y no se pudo reiniciar):

```bash
$ for p in /cuestionario /cuestionario/quiz /cuestionario/resumenes \
           /cuestionario/clase/1 /cuestionario/clase/6 \
           /cuestionario/pdf/clase/1 /cuestionario/pdf/clase/6 \
           /cuestionario/clase/9 /cuestionario/pdf/clase/7 \
           /cuestionario.html /; do
    curl -s -o /dev/null -w "%{http_code}  $p\n" http://localhost:7080$p
  done

200  /cuestionario
200  /cuestionario/quiz
200  /cuestionario/resumenes
200  /cuestionario/clase/1
200  /cuestionario/clase/6
200  /cuestionario/pdf/clase/1
200  /cuestionario/pdf/clase/6    # sirve desde clases/clase5/ (donde vive el PDF)
404  /cuestionario/clase/9        # validacion estricta 1..6
404  /cuestionario/pdf/clase/7
302  /cuestionario.html           # redirige a /cuestionario (compat)
200  /                            # SPA del TPO sigue funcionando
```

**Magic bytes de los PDFs** — los 6 devuelven `%PDF` como primeros 4 bytes, con tamaños distintos (no hay colision; el handler matchea "Clase N" o "Clase N " en el nombre del archivo):

| Clase | Tamaño |
|-------|--------|
| 1     | 127.525 B |
| 2     | 143.116 B |
| 3     | 166.861 B |
| 4     | 148.614 B |
| 5     | 113.679 B |
| 6     | 127.871 B |

**Contenido HTML verificado:**

- Hub: `<title>Material de estudio · AyED II · UADE 2026</title>`
- Quiz: 6 unidades (`Clase 1 — Repaso de Java` a `Clase 6 — TADs del dominio, archivos y JSON`), 62 preguntas totales (count de `correct:`).
- Cada `/cuestionario/clase/N` renderiza el `<h1>` correcto.
- Redirect legacy `/cuestionario.html` → `302 Location: /cuestionario`.

**Regresion del SPA principal:**

- `GET /` devuelve `<title>Logi-UADE — Gestión logística</title>` intacto.
- `GET /api/estado` responde el JSON KPI esperado.

**Lo que queda pendiente de verificar visualmente** (no habia sesion RDP activa al momento; el usuario tiene que probar con su navegador):

- [ ] Renderizado de las 3 hub cards en `/cuestionario` en ambos temas (claro/oscuro).
- [ ] Persistencia del quiz en `localStorage` (responder → recargar → ver respuestas conservadas).
- [ ] Botones "Mostrar todas las respuestas" y "Reiniciar".
- [ ] Descarga del PDF via atributo `download="AyED-II-clase-N.pdf"`.
- [ ] Navegacion prev/next entre resumenes.

**Screenshots pendientes** en `tmp/pruebas-ui/` cuando se corra la prueba visual.

---

## Anexo — Material de exposición grupal en `/cuestionario/exposicion` (2026-04-23)

Agregadas dos páginas nuevas para defender el TPO en la evaluación oral:

- `/cuestionario/exposicion` — hub local con 2 cards (Arquitectura · Teórica) y tabla de distribución por persona.
- `/cuestionario/exposicion/arquitectura` — 6 secciones (una por persona) con talking points, código de referencia y preguntas probables del jurado.
- `/cuestionario/exposicion/teorica` — 6 secciones con cita literal de la consigna, complejidades documentadas y Q&A.

**Cambios en código:**

- `WebServer.java`: 3 aliases nuevos en `StaticHandler` (mismo patrón que `/cuestionario/resumenes`).
- `web/cuestionario/index.html`: card nueva en el hub-grid apuntando a `/cuestionario/exposicion` + link en navbar.
- `web/cuestionario/*.html` (quiz, resumenes, clase1-6): link `🎤 Exposición` agregado al navbar para navegación consistente.
- `web/cuestionario/assets/estudio.css`: reglas nuevas para `.code-refs`, `.qa` y `blockquote` (~35 líneas al final del archivo).

**Smoke test con curl (post `docker compose up -d --build`):**

| Ruta | Status | Size |
|------|--------|------|
| `GET /cuestionario/exposicion` | `200` | 4.9 KB |
| `GET /cuestionario/exposicion/arquitectura` | `200` | 28.0 KB |
| `GET /cuestionario/exposicion/teorica` | `200` | 34.3 KB |
| `GET /cuestionario/exposicion/arquitectura` Content-Type | `text/html; charset=utf-8` | — |

**Contenido verificado via curl:**

- Arquitectura: 6 anchors `#parte-1` a `#parte-6`, Personas A–F presentes.
- Teórica: 6 anchors `#parte-1` a `#parte-6`, Personas A–F, mención a "Dijkstra" en parte 6.
- Hub: cards "Exposición de arquitectura", "Defensa teórica de los TDAs" y "Distribución sugerida" renderizan.

**Regresión post-deploy:**

- `GET /` → `200` (SPA del TPO).
- `GET /cuestionario` → `200` (hub ya muestra la card de Exposición).
- `GET /cuestionario/quiz` → `200`.
- `GET /cuestionario/resumenes` → `200`.
- `GET /cuestionario/clase/1` → `200`.
- `GET /cuestionario/pdf/clase/3` → `200` (PDF original intacto).

**Pendiente de verificar visualmente** (sin RDP activo al momento):

- [ ] Render de las 4 hub cards en `/cuestionario` (claro/oscuro) — ahora hay 4 cards en vez de 3.
- [ ] Render de las 6 secciones numeradas de arquitectura y teórica, con `.qa` (Q&A) y `blockquote` (citas).
- [ ] Dark mode aplicado correctamente a los nuevos componentes.
- [ ] Anchors del índice que llevan a cada parte (`#parte-N`).
- [ ] Navegación prev/next entre arquitectura ↔ teórica ↔ hub.

**Screenshots pendientes en `tmp/pruebas-ui/`:**

- `exposicion-hub.png`, `exposicion-arquitectura.png`, `exposicion-teorica.png` (dark).
- Mismas 3 en light mode.

