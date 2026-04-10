# Flujos de Logi-UADE 2026

Esta guia describe, uno por uno, los flujos funcionales que expone la aplicacion web de Logi-UADE 2026. Cada flujo combina una interaccion en la UI con uno o mas endpoints REST del backend Java (`LogisticaService` + TDAs). Toda la logica vive en el backend: el front-end solo hace `fetch()` y renderiza.

Convencion: cada flujo lista el **gatillo** (que hace el usuario en la UI), el **endpoint** que se invoca, la **TDA** involucrada y el **resultado esperado**.

---

## 1. Cargar inventario

- **Gatillo**: click en el boton `Cargar inventario` del header.
- **Endpoint**: `POST /api/inventario/cargar` con body `{ "path": "data/inventario.json" }`.
- **Backend**: `LogisticaService.cargarInventario(Path)` resetea el estado, delega en `InventarioLoader` (parser JSON manual) y popula `CentroDistribucion`, `ArbolDepositos` y `GrafoDepositos`.
- **TDAs tocadas**: `PriorityQueue` del centro, ABB de depositos, grafo de rutas.
- **Resultado esperado**: toast con la cantidad de paquetes / depositos / rutas, KPIs actualizados (Pendientes = 3), y entrada en el log de actividad.

## 2. Alta manual de paquete

- **Gatillo**: form `Centro de distribucion` → ingresar `ID`, `Peso`, `Destino`, `Contenido` (opcional) y check `Marcar como urgente`. Click `Dar de alta`.
- **Endpoint**: `POST /api/centro/paquetes` con el body del form.
- **Backend**: `LogisticaService.crearPaqueteManual(...)` construye un `Paquete<String>` y lo inserta en el `CentroDistribucion`.
- **TDA**: `PriorityQueue` (cola prioritaria).
- **Regla de negocio**: paquetes urgentes o con peso `> 50 kg` tienen prioridad. Desempate por orden de llegada (FIFO).
- **Resultado esperado**: toast de exito, KPI `Pendientes` incrementado en 1, form limpio.

## 3. Procesar siguiente al camion

- **Gatillo**: click en `Procesar siguiente → camion`.
- **Endpoint**: `POST /api/camion/cargar` (sin body).
- **Backend**: `procesarSiguienteEnCentro()` hace `poll()` en la cola prioritaria y `cargarEnCamion(p)` lo empuja al camion.
- **TDAs**: `PriorityQueue` → `Stack` (LIFO).
- **Resultado esperado**: el paquete mas prioritario sale del centro y queda como nuevo tope del camion. Complejidad: `O(log n)` en la cola, `O(1)` en la pila.

## 4. Ver carga del camion

- **Gatillo**: automatico en cada refresco de estado (lo hace `refrescarEstado()` despues de cada accion).
- **Endpoint**: `GET /api/camion` → `{ cantidad, paquetes }`.
- **Backend**: `verCargaCamion()` devuelve una copia del `ArrayDeque` con el tope primero.
- **Resultado esperado**: lista ordenada con el tope al principio, chips `urgente` y `TOPE` cuando corresponde.

## 5. Deshacer ultima carga (undo)

- **Gatillo**: click en `Deshacer ultima` (card del Camion).
- **Endpoint**: `POST /api/camion/deshacer`.
- **Backend**: `deshacerUltimaCargaCamion()` hace `pop()` y devuelve el paquete. Es la misma operacion que `descargar`, pero semantica de "me equivoque". Complejidad `O(1)`.
- **Resultado esperado**: el tope desaparece de la lista, el KPI `Camion` baja en 1.

## 6. Descargar tope del camion

- **Gatillo**: click en `Descargar tope`.
- **Endpoint**: `POST /api/camion/descargar`.
- **Backend**: `descargarCamion()` → `pop()`.
- **Resultado esperado**: igual que undo (en este modelo son equivalentes; la diferencia es solo semantica de UI).

## 7. Auditoria de depositos

- **Gatillo**: opcional → setear `Fecha referencia` en el datetime-local. Click `Auditar`.
- **Endpoint**: `POST /api/depositos/auditar` con body `{ "fechaReferencia": "ISO" }` o vacio (usa `now()`).
- **Backend**: `ArbolDepositos.auditarDepositosPendientes(fecha)` hace un **recorrido post-orden** y marca como `visitado = true` a los depositos cuya `fechaUltimaAuditoria` es `null` o anterior a `fecha - 30 dias`. Devuelve los IDs marcados en el orden post-orden.
- **TDA**: ABB manual.
- **Resultado esperado**: lista de IDs en orden post-orden. Con fecha `2026-04-10` y el inventario de ejemplo, el resultado es `[10, 30, 90, 80, 50]`.

## 8. Consultar depositos por nivel

- **Gatillo**: ingresar `Nivel del arbol` (0 = raiz). Click `Consultar nivel`.
- **Endpoint**: `GET /api/depositos/nivel/{n}`.
- **Backend**: `ArbolDepositos.obtenerNivel(n)` hace un **BFS por niveles** sobre el ABB.
- **TDA**: ABB + cola (`ArrayDeque`).
- **Resultado esperado**: lista de depositos del nivel N. Para el inventario de ejemplo:
  - Nivel 0 → `50 Hub Central Buenos Aires`
  - Nivel 1 → `20 Deposito Cordoba`, `80 Deposito Mendoza`
  - Nivel 2 → `10`, `30`, `90`

## 9. Calcular distancia minima

- **Gatillo**: ingresar `Origen` y `Destino` (IDs de depositos). Click `Calcular distancia`.
- **Endpoint**: `GET /api/rutas/distancia?origen=X&destino=Y`.
- **Backend**: `GrafoDepositos.distanciaMinima(origen, destino)` corre **Dijkstra** sobre la lista de adyacencia. Complejidad `O((V + E) log V)`, espacio `O(V)`.
- **TDA**: grafo no dirigido con lista de adyacencia + `PriorityQueue` para la frontera.
- **Resultado esperado**: distancia minima en km. Ejemplo: origen `10`, destino `90` → **2050 km** (ruta `10→20→30→90 = 740+330+980`).

## 10. Panel de KPIs y log de actividad

- **Gatillo**: se refresca solo despues de cualquier accion que muta estado.
- **Endpoint**: `GET /api/estado` + `GET /api/camion`.
- **Backend**: lecturas de `cantidadPendientesCentro()`, `cantidadPaquetesEnCamion()` y tope (`verCargaCamion().get(0)`).
- **Resultado esperado**: los cuatro KPIs del tope (Centro / Camion / Tope / Endpoint) reflejan el estado actual. El log inverso (mas reciente arriba) acumula las acciones con timestamp.

---

## Apendice: endpoints REST

| Metodo | Ruta                                           | Flujo |
|--------|------------------------------------------------|-------|
| POST   | `/api/inventario/cargar`                       | 1     |
| GET    | `/api/estado`                                  | 10    |
| POST   | `/api/centro/paquetes`                         | 2     |
| POST   | `/api/camion/cargar`                           | 3     |
| POST   | `/api/camion/deshacer`                         | 5     |
| POST   | `/api/camion/descargar`                        | 6     |
| GET    | `/api/camion`                                  | 4, 10 |
| POST   | `/api/depositos/auditar`                       | 7     |
| GET    | `/api/depositos/nivel/{n}`                     | 8     |
| GET    | `/api/depositos/{id}`                          | (util) |
| GET    | `/api/rutas/distancia?origen=X&destino=Y`      | 9     |
