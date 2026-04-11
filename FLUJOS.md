# Flujos de Logi-UADE 2026

Guia funcional de la aplicacion web. Cada flujo indica la vista donde ocurre, la accion del usuario, el endpoint REST, la estructura de datos (TDA) involucrada y el resultado esperado.

La aplicacion es una SPA con sidebar fijo y ruteo por hash. Las vistas disponibles son `#/inicio`, `#/paquetes`, `#/camion`, `#/depositos`, `#/rutas` y `#/guia`. Toda la logica vive en Java (`LogisticaService` + TDAs). El front-end solo hace `fetch()` contra `/api/*` y renderiza.

---

## 1. Cargar inventario

- **Vista**: boton `Cargar inventario` en el header (disponible desde cualquier pagina) o card `Empezá acá` en la vista Inicio.
- **Endpoint**: `POST /api/inventario/cargar` con body `{ "path": "data/inventario.json" }`.
- **Backend**: `LogisticaService.cargarInventario(Path)` reinicia el estado interno (centro, camion, ABB, grafo), delega en `InventarioLoader` (parser JSON manual) y popula los TDAs.
- **TDAs tocadas**: cola prioritaria del centro, ABB de depositos, grafo de rutas.
- **Resultado esperado**: toast verde con el resumen (`3 paquetes, 6 depositos, 6 rutas`). El sidebar pasa a `Operando`. KPIs de Inicio muestran 3 / 0 / 6 / 6. Linea en el feed de actividad.

## 2. Registrar paquete manual

- **Vista**: Paquetes → card izquierda `Registrar paquete`.
- **Accion**: completar `ID`, `Peso (kg)`, `Destino`, `Contenido` (opcional) y marcar `Urgente` si corresponde. Click `Registrar paquete`.
- **Endpoint**: `POST /api/centro/paquetes`.
- **Backend**: `LogisticaService.crearPaqueteManual(...)` construye un `Paquete<String>` y lo inserta en el centro.
- **TDA**: `PriorityQueue` (cola con prioridad).
- **Regla de negocio**: paquetes urgentes o de mas de `50 kg` son prioritarios (desempate FIFO por orden de llegada). El backend marca `prioritario=true` en el payload para que el front-end pueda etiquetar el badge sin replicar la regla.
- **Resultado esperado**: toast `Paquete X registrado`. El paquete aparece en la lista `Proximos a cargar` con badge `Urgente`, `Prioritario` o `Normal`. KPI `Paquetes pendientes` incrementa.

## 3. Enviar siguiente al camion

- **Vista**: Paquetes (boton `Enviar siguiente`) o Inicio (accion rapida `Enviar siguiente al camion`).
- **Endpoint**: `POST /api/camion/cargar`.
- **Backend**: `procesarSiguienteEnCentro()` hace `poll()` en la cola prioritaria y `cargarEnCamion(p)` lo apila.
- **TDAs**: `PriorityQueue` → pila LIFO (`ArrayDeque`).
- **Complejidad**: `O(log n)` en el centro, `O(1)` en el camion.
- **Resultado esperado**: toast `Enviado: PKG-XXX`. El paquete sale del centro y pasa a ser el nuevo tope del camion (etiqueta `Siguiente a entregar` en la vista Camion).

## 4. Ver carga del camion

- **Vista**: Camion (se dispara al navegar o despues de cualquier accion de camion).
- **Endpoint**: `GET /api/camion` → `{ cantidad, pesoTotal, paquetes }`.
- **Backend**: `verCargaCamion()` devuelve una copia del `ArrayDeque` con el tope como primer elemento. `pesoTotal()` suma los pesos en Java.
- **Resultado esperado**: lista ordenada con el tope arriba (etiqueta `Siguiente a entregar`, badge `Urgente` si corresponde). Tarjetas laterales muestran `Total cargado` (cantidad) y `Peso total` (en kg) calculados por el backend.

## 5. Deshacer ultima carga

- **Vista**: Camion → boton `Deshacer`.
- **Endpoint**: `POST /api/camion/deshacer`.
- **Backend**: `deshacerUltimaCargaCamion()` hace `pop()` de la pila y devuelve el paquete. Semantica de "me equivoque al cargar".
- **Complejidad**: `O(1)`.
- **Resultado esperado**: el paquete del tope desaparece de la lista. KPIs `Camion` y `Peso total` se actualizan. Toast info con el ID deshecho.

## 6. Descargar tope del camion

- **Vista**: Camion → boton `Descargar`.
- **Endpoint**: `POST /api/camion/descargar`.
- **Backend**: `descargarCamion()` → `pop()`. Semantica de "entrega en destino".
- **Resultado esperado**: mismo comportamiento fisico que `Deshacer` (ambos son `pop()` en `O(1)`), pero diferenciado por log/toast para dejar clara la intencion.

## 7. Ejecutar auditoria de depositos

- **Vista**: Depositos → card superior con selector `Fecha de referencia` y boton `Ejecutar auditoria`.
- **Endpoint**: `POST /api/depositos/auditar` con body `{ "fechaReferencia": ISO }` (si se deja vacio usa `LocalDateTime.now()`).
- **Backend**: `ArbolDepositos.auditarDepositosPendientes(fecha)` recorre el ABB en **post-orden** y marca como `visitado = true` los depositos cuya `fechaUltimaAuditoria` es `null` o anterior a `fecha - 30 dias`. Devuelve los IDs marcados en el orden del recorrido.
- **TDA**: ABB manual.
- **Resultado esperado**: banner amarillo con la cantidad de depositos marcados y los IDs (`[10, 30, 90, 80, 50]` con fecha `2026-04-10`). En la lista de depositos, cada uno afectado pasa a mostrar badge `Requiere atencion` en lugar de `Al dia`.

## 8. Ver red de depositos

- **Vista**: Depositos → lista inferior.
- **Endpoint**: `GET /api/depositos` → `{ cantidad, depositos }`.
- **Backend**: `ArbolDepositos.listarTodos()` hace un recorrido **in-order** del ABB → devuelve los depositos ordenados ascendente por ID.
- **Resultado esperado**: 6 cards con `ID`, `nombre`, `fecha relativa` (ej. "hace 3 meses"), y badge `Al dia` o `Requiere atencion` segun la ultima auditoria. El orden es 10, 20, 30, 50, 80, 90 (in-order del ABB con raiz 50).

## 9. Calcular ruta mas corta

- **Vista**: Rutas.
- **Accion**: elegir `Origen` y `Destino` (dropdowns con nombre+ID, no inputs crudos) → click `Calcular`.
- **Endpoint**: `GET /api/rutas/distancia?origen=X&destino=Y`.
- **Backend**: `GrafoDepositos.distanciaMinima(o, d)` corre **Dijkstra** sobre la lista de adyacencia.
- **Complejidad**: `O((V + E) log V)` tiempo, `O(V)` espacio.
- **Resultado esperado**: card con gradient mostrando `N km`, ademas del nombre de origen → destino. Ejemplo: Salta → Neuquen = `2050 km` (ruta `10 → 20 → 30 → 90 = 740 + 330 + 980`).

Tambien la vista carga `GET /api/rutas` y lista las 6 aristas del grafo en el panel lateral `Conexiones disponibles`.

## 10. Guia de uso y navegacion

- **Vista**: sidebar (desktop) o menu hamburguesa (mobile) + `#/guia`.
- **Accion**: navegar entre las 6 vistas. Abrir `Como usar` para ver la guia paso a paso.
- **Resultado esperado**: el sidebar resalta el item activo (bg `brand-soft`, texto `brand`). El titulo + subtitulo del header cambian segun la ruta. La vista Guia muestra 6 tarjetas numeradas en lenguaje llano y un `<details>` expandible `Sobre las estructuras internas` con los detalles academicos (TDAs usados, complejidades).

---

## Flujos de API no expuestos en la UI

Hay un endpoint que existe en el backend por requisito de la consigna pero no aparece en la UI de produccion porque no tiene valor operativo para un usuario final:

- **Depositos por nivel (BFS)**: `GET /api/depositos/nivel/{n}`.
  - Backend: `ArbolDepositos.obtenerNivel(n)` hace BFS por niveles sobre el ABB.
  - Resultado para el dataset de ejemplo: nivel 0 → `[50]`, nivel 1 → `[20, 80]`, nivel 2 → `[10, 30, 90]`.
  - Testeable via `curl`, ver PRUEBAS.md.

---

## Apendice: endpoints REST

| Metodo | Ruta                                           | Flujo |
|--------|------------------------------------------------|-------|
| POST   | `/api/inventario/cargar`                       | 1     |
| GET    | `/api/estado`                                  | 1, 3-6 |
| GET    | `/api/centro/paquetes`                         | 2, 3  |
| POST   | `/api/centro/paquetes`                         | 2     |
| POST   | `/api/camion/cargar`                           | 3     |
| GET    | `/api/camion`                                  | 4     |
| POST   | `/api/camion/deshacer`                         | 5     |
| POST   | `/api/camion/descargar`                        | 6     |
| POST   | `/api/depositos/auditar`                       | 7     |
| GET    | `/api/depositos`                               | 8     |
| GET    | `/api/depositos/{id}`                          | (util)|
| GET    | `/api/depositos/nivel/{n}`                     | (API) |
| GET    | `/api/rutas`                                   | 9     |
| GET    | `/api/rutas/distancia?origen=X&destino=Y`      | 9     |
