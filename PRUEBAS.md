# Resultado de pruebas manuales — Logi-UADE 2026

Pruebas end-to-end de la UI web ejecutadas sobre `http://192.168.1.101:7070/` con Chromium (Playwright MCP, modo headed sobre RDP). Los screenshots viven en `tmp/pruebas-ui/`.

- **Entorno**: `java -cp out ar.edu.uade.logistica.Main` (servidor HTTP JDK, puerto 7070).
- **Dataset**: `data/inventario.json` — 3 paquetes, 6 depositos, 6 rutas.
- **Fecha de ejecucion**: 2026-04-10.

Ver `FLUJOS.md` para la descripcion funcional de cada flujo.

---

## Flujo 1 — Cargar inventario

**Resultado: ✅ PASS**

- **Accion**: click en `Cargar inventario` del header.
- **Estado observado**:
  - KPI `Pendientes` = **3** (antes: 1, con un PKG-999 residual de pruebas previas).
  - KPI `Camion` = **0** (estado se reinicio correctamente).
  - Log: `Inventario cargado desde data/inventario.json`.
  - Toast verde: `Inventario: 3 paquetes, 6 depositos, 6 rutas`.
- **Verificacion**: el servicio reinicia `Camion`, `CentroDistribucion`, `ArbolDepositos` y `GrafoDepositos` antes de poblar. El estado previo (PKG-999, camion con 2 paquetes) desaparece, confirmando el reset.
- **Screenshots**:
  - `tmp/pruebas-ui/01a-inicial.png` — antes del click (estado residual).
  - `tmp/pruebas-ui/01b-cargado.png` — post-carga, pendientes=3, camion=0.

---

## Flujo 2 — Alta manual de paquete

**Resultado: ✅ PASS**

- **Accion**: form con `ID=PKG-TEST`, `Peso=25`, `Destino=Mar del Plata`, `Contenido=Medicamentos`, `Urgente=true`. Click `Dar de alta`.
- **Estado observado**:
  - KPI `Pendientes` paso de **3 → 4**.
  - Log: `Alta manual PKG-TEST (Mar del Plata, 25 kg, urgente)`.
  - Form reseteado (inputs vacios, checkbox desmarcado).
- **Verificacion**: el backend construyo el `Paquete<String>` (peso < 50 pero urgente = prioritario) y lo inserto en la `PriorityQueue` del `CentroDistribucion`.
- **Screenshots**:
  - `tmp/pruebas-ui/02a-form-completo.png` — form lleno antes del submit.
  - `tmp/pruebas-ui/02b-alta-ok.png` — post-alta, pendientes=4, form limpio.

---

## Flujo 3 — Procesar siguiente al camion (prioridad)

**Resultado: ✅ PASS**

- **Accion**: click `Procesar siguiente → camion` 4 veces consecutivas.
- **Orden observado** (entrada a la pila en ese orden):
  1. `PKG-002` — Cordoba, 72 kg → prioritario por **peso > 50 kg**.
  2. `PKG-003` — Rosario, 8.2 kg, urgente → prioritario por **urgente**.
  3. `PKG-TEST` — Mar del Plata, 25 kg, urgente → prioritario por **urgente**.
  4. `PKG-001` — Buenos Aires, 12.5 kg, normal → **ultimo**, sin prioridad.
- **Estado final**:
  - KPI `Pendientes` = 0, `Camion` = 4, `Tope` = PKG-001.
  - Log con las 4 lineas en orden inverso (mas reciente arriba).
- **Verificacion**: la `PriorityQueue` sacó los 3 prioritarios (peso > 50 o urgentes) antes que el normal. Entre prioritarios, el orden fue **FIFO por llegada** (desempate por `ordenLlegada`), correcto. La pila del camion termina con `PKG-001` arriba (ultimo en entrar).
- **Screenshot**: `tmp/pruebas-ui/03-camion-lleno.png`.

---

## Flujo 4 — Ver carga del camion (LIFO)

**Resultado: ✅ PASS**

- **Accion**: inspeccion visual del panel `Camion (pila LIFO)` tras el flujo 3.
- **Orden observado** (tope arriba):
  1. `PKG-001` — Buenos Aires, 12.5 kg — **TOPE** (ultimo en entrar)
  2. `PKG-TEST` — Mar del Plata, 25 kg — chip `urgente`
  3. `PKG-003` — Rosario, 8.2 kg — chip `urgente`
  4. `PKG-002` — Cordoba, 72 kg — sin chip (prioritario por peso, no por urgencia)
- **Verificacion**: el orden visual confirma LIFO puro — el ultimo insertado queda en el tope y coincide con el KPI `Tope del camion = PKG-001`. Los chips `urgente` se pintan solo sobre los paquetes marcados como urgentes (PKG-TEST y PKG-003); PKG-002 entro por peso pero no lleva chip de urgencia, correcto.
- **Screenshot**: `tmp/pruebas-ui/04-camion-lifo.png`.

---

## Flujo 5 — Deshacer ultima carga

**Resultado: ✅ PASS**

- **Accion**: click `Deshacer ultima` con el camion en el estado del flujo 4.
- **Estado observado**:
  - KPI `Camion` paso de **4 → 3**.
  - KPI `Tope` paso de `PKG-001` a `PKG-TEST`.
  - `PKG-001` desaparecio de la lista.
  - Log: `Undo tope camion: PKG-001`.
- **Verificacion**: el `pop()` del `ArrayDeque` saco exactamente el elemento que estaba en el tope, en `O(1)`. El resto de la pila quedo intacta.
- **Screenshot**: `tmp/pruebas-ui/05-undo.png`.

---

## Flujo 6 — Descargar tope del camion

**Resultado: ✅ PASS**

- **Accion**: click `Descargar tope` con el camion en `[PKG-TEST, PKG-003, PKG-002]`.
- **Estado observado**:
  - KPI `Camion` paso de **3 → 2**.
  - KPI `Tope` paso de `PKG-TEST` a `PKG-003`.
  - Log: `Descarga camion: PKG-TEST`.
- **Verificacion**: mismo comportamiento que `Undo` (tambien es `pop()`). La UI diferencia los logs por tipo de accion (`Descarga camion:` vs `Undo tope camion:`) pero la pila reacciona igual.
- **Screenshot**: `tmp/pruebas-ui/06-descarga.png`.

---

## Flujo 7 — Auditoria de depositos (post-orden)

**Resultado: ✅ PASS**

- **Accion**: `Fecha referencia = 2026-04-10T00:00`, click `Auditar`.
- **Estado observado**:
  - Resultado renderizado: `Referencia: 2026-04-10T00:00 / IDs visitados: [10, 30, 90, 80, 50]`.
- **Verificacion**: limite de 30 dias → `2026-03-11`. Fechas del dataset:
  - `50` (raiz) — `2026-01-10` — anterior al limite → visitado ✓
  - `20` — `2026-03-20` — posterior al limite → **NO visitado** ✓
  - `10` — `null` — sin auditoria previa → visitado ✓
  - `30` — `2026-03-01` — anterior → visitado ✓
  - `80` — `2025-12-01` — anterior → visitado ✓
  - `90` — `2026-02-01` — anterior → visitado ✓
  - Orden **post-orden** esperado: `[10, 30, 90, 80, 50]` (el `20` no se marca pero igual se visita en el recorrido).
  - Resultado del servidor coincide byte por byte ✓.
- **Screenshot**: `tmp/pruebas-ui/07-auditoria.png`.

---

## Flujo 8 — Consultar depositos por nivel (BFS)

**Resultado: ✅ PASS**

Probado con 3 niveles consecutivos:

- **Nivel 0** → `50 — Hub Central Buenos Aires` (raiz). Chip `visitado` (auditado en flujo 7). ✓
- **Nivel 1** → `20 — Deposito Cordoba`, `80 — Deposito Mendoza`. Solo el 80 con chip `visitado` (el 20 tiene fecha posterior al limite de auditoria). ✓
- **Nivel 2** → `10 — Deposito Salta`, `30 — Deposito Santa Fe`, `90 — Deposito Neuquen`. Los tres con chip `visitado`. ✓

- **Verificacion**: el BFS por niveles del ABB devolvio exactamente los depositos esperados segun la estructura de insercion (raiz 50, luego 20 < 50, 80 > 50, etc.). Los chips `visitado` son coherentes con el resultado del flujo 7.
- **Screenshot**: `tmp/pruebas-ui/08-niveles.png`.

---

## Flujo 9 — Calcular distancia minima (Dijkstra)

**Resultado: ✅ PASS**

Dos consultas encadenadas:

- **10 → 90** → resultado: `2050 km`.
  - Ruta optima esperada: `10 → 20 → 30 → 90 = 740 + 330 + 980 = 2050`. ✓
- **50 → 90** → resultado: `1570 km`.
  - Ruta optima esperada: `50 → 30 → 90 = 590 + 980 = 1570` (mas corta que `50 → 80 → 90 = 1050 + 520 = 1570`, empate — Dijkstra elige cualquiera, ambas cuestan igual). ✓

- **Verificacion**: la implementacion con `PriorityQueue` sobre lista de adyacencia devuelve el camino minimo en `O((V + E) log V)`. Los dos casos dan los valores esperados.
- **Screenshot**: `tmp/pruebas-ui/09-dijkstra.png`.

---

## Flujo 10 — KPIs y log de actividad

**Resultado: ✅ PASS**

- **Estado final de los KPIs**:
  - `Pendientes` = 0
  - `Camion` = 2
  - `Tope` = `PKG-003`
  - `Endpoint` = `localhost:7070` (chip `online` verde)
- **Log completo de la sesion de pruebas** (mas reciente arriba, 14 entradas):
  1. `Dijkstra 50→90 = 1570 km`
  2. `Dijkstra 10→90 = 2050 km`
  3. `Nivel 2: 3 depositos`
  4. `Nivel 1: 2 depositos`
  5. `Nivel 0: 1 depositos`
  6. `Auditoria: 5 depositos marcados`
  7. `Descarga camion: PKG-TEST`
  8. `Undo tope camion: PKG-001`
  9. `Centro → camion: PKG-001 (Buenos Aires)`
  10. `Centro → camion: PKG-TEST (Mar del Plata)`
  11. `Centro → camion: PKG-003 (Rosario)`
  12. `Centro → camion: PKG-002 (Cordoba)`
  13. `Alta manual PKG-TEST (Mar del Plata, 25 kg, urgente)`
  14. `Inventario cargado desde data/inventario.json`
- **Verificacion**: cada accion disparada en los flujos 1-9 quedo registrada con timestamp y en orden inverso. Los KPIs reflejan exactamente el estado final (post-undo, post-descarga). Las consultas de solo-lectura (auditoria, nivel, Dijkstra) no alteraron los KPIs de inventario/camion.
- **Screenshot**: `tmp/pruebas-ui/10-kpis-log.png`.

---

## Resumen

| # | Flujo                                           | Resultado |
|---|-------------------------------------------------|-----------|
| 1 | Cargar inventario                               | ✅ PASS   |
| 2 | Alta manual de paquete                          | ✅ PASS   |
| 3 | Procesar siguiente al camion (prioridad)        | ✅ PASS   |
| 4 | Ver carga del camion (LIFO)                     | ✅ PASS   |
| 5 | Deshacer ultima carga (undo)                    | ✅ PASS   |
| 6 | Descargar tope del camion                       | ✅ PASS   |
| 7 | Auditoria de depositos (post-orden)             | ✅ PASS   |
| 8 | Consultar depositos por nivel (BFS)             | ✅ PASS   |
| 9 | Calcular distancia minima (Dijkstra)            | ✅ PASS   |
| 10 | KPIs y log de actividad                         | ✅ PASS   |

**10/10 flujos en verde.** Ningun bug observado. Unica console warning: `favicon.ico 404` (cosmetico).

