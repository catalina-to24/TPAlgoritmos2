# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

Logi-UADE 2026 es un Trabajo Practico Obligatorio (TPO) de AyED II / Programacion II (UADE). La consigna original esta en `Consigna.pdf`. El proyecto implementa un sistema de gestion logistica con TDAs construidos desde cero (pila, cola prioritaria, ABB manual, grafo con lista de adyacencia) y un front-end web. Es un TPO academico — la rubrica pesa **30% Eficiencia** (complejidad documentada), por lo que cada operacion principal de los TDAs tiene Javadoc con `@implNote Complejidad: O(...) tiempo / O(...) espacio`. No romper esos comentarios.

**Requisitos de la consigna que son dogma** (no "refactorizar" ni "simplificar"):

- `Paquete<T>` debe ser **generico**.
- El camion debe ser una **pila (LIFO)** y el undo debe ser `O(1)`.
- El centro de distribucion debe ser una **cola prioritaria** donde urgentes o `peso > 50 kg` tienen prioridad, con desempate FIFO por orden de llegada.
- El ABB de depositos **debe estar implementado a mano** (no `TreeMap`).
- La auditoria es en **post-orden** y marca como `visitado = true` a los que **NO** fueron auditados en los ultimos 30 dias. Esta semantica es literal de la consigna — la logica en `ArbolDepositos.auditarPostOrden` esta correcta y ya fue validada contra el texto del PDF; **no la cambies** aunque parezca invertida.
- Reporte por nivel N con **BFS**.
- Grafo con **lista de adyacencia** y distancia minima (Dijkstra porque el JSON trae km).
- El parser JSON (`SimpleJsonParser`) y el loader de inventario son **caseros** — la consigna pide evitar dependencias externas.

## Build, run, tests

El proyecto no usa Maven/Gradle. Se compila con `javac` y un directorio `out/`. No hay dependencias externas para el main — solo JUnit standalone en `lib/` para los tests.

```bash
# Compilar todo el main
mkdir -p out
javac -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/web out/   # estaticos para el WebServer

# Arrancar el server web (default, puerto 7070, bindea a 0.0.0.0)
java -cp out ar.edu.uade.logistica.Main
# Otro puerto:
java -cp out ar.edu.uade.logistica.Main --port 8080

# Modo CLI (menu por consola, sin servidor)
java -cp out ar.edu.uade.logistica.Main --cli

# Compilar y correr tests (JUnit 5 standalone, sin Maven)
mkdir -p out-test
javac -cp "out:lib/junit-platform-console-standalone-1.11.3.jar" -d out-test $(find src/test/java -name "*.java")
java -jar lib/junit-platform-console-standalone-1.11.3.jar execute \
  -cp "out:out-test" --scan-classpath --disable-banner

# Correr un unico test especifico
java -jar lib/junit-platform-console-standalone-1.11.3.jar execute \
  -cp "out:out-test" --disable-banner \
  --select-class=ar.edu.uade.logistica.structures.ArbolDepositosTest
# O un unico metodo:
java -jar lib/junit-platform-console-standalone-1.11.3.jar execute \
  -cp "out:out-test" --disable-banner \
  --select-method=ar.edu.uade.logistica.structures.CamionTest#deshacerDevuelvePaqueteTope
```

Java 21 o superior. El JDK **completo** (`openjdk-21-jdk`, no headless) solo era necesario para la antigua GUI Swing. Ahora con el web server headless alcanza.

## Arquitectura

Capas, de adentro hacia afuera:

```
model/        → Entidades puras (Paquete<T>, Deposito, Ruta, Inventario)
structures/   → TDAs construidos desde cero (Camion, CentroDistribucion,
                ArbolDepositos, GrafoDepositos). Cada metodo publico
                tiene Javadoc con complejidad.
persistence/  → SimpleJsonParser (parser manual) + InventarioLoader
service/      → LogisticaService: orquesta los TDAs. Es el unico
                punto de entrada a la logica del dominio.
cli/          → MenuConsola (modo --cli)
web/          → WebServer + JsonWriter (modo default). Usa com.sun.net.httpserver
                de la JDK — cero dependencias externas.
```

Regla dura: **toda la logica de negocio vive en Java** (service + structures). El front-end `src/main/resources/web/` es HTML + Tailwind (CDN) + JS vanilla que solo hace `fetch()` contra `/api/*` y renderiza. Si te encontras haciendo un calculo en JS, esta mal — subilo al backend.

**Puntos de entrada**:

- `Main.java` → routea entre `WebServer.start(port)` (default) y `MenuConsola.iniciar()` (`--cli`).
- `WebServer` instancia **un solo** `LogisticaService` por proceso y sincroniza las mutaciones con `synchronized (service)`. Jetty... perdon, `HttpServer` de la JDK puede lanzar varios workers, por eso el lock.
- `LogisticaService` es el unico que sabe como coordinar `Camion`, `CentroDistribucion`, `ArbolDepositos` y `GrafoDepositos`. CLI y Web ambos le hablan a el — ninguna logica se duplica.

**Formato de datos** (`data/inventario.json`):

```json
{
  "paquetes":  [{ "id", "peso", "destino", "contenido", "urgente" }],
  "depositos": [{ "id", "nombre", "visitado", "fechaUltimaAuditoria" }],
  "rutas":     [{ "origen", "destino", "distanciaKm" }]
}
```

No tiene campo `conexiones` — fue eliminado en el refactor porque estaba duplicado con `rutas`. El parser actualmente **no** tolera ese campo si lo agregas de nuevo.

## API REST

Expuesta por `WebServer`. Todos los endpoints responden JSON via `JsonWriter` (escritor casero, simetrico con `SimpleJsonParser`, sin Jackson).

| Metodo | Ruta                                           | Mapea a                                |
|--------|------------------------------------------------|----------------------------------------|
| POST   | `/api/inventario/cargar`                       | `service.cargarInventario(Path)`       |
| GET    | `/api/estado`                                  | KPIs + totales + `inventarioCargado`   |
| GET    | `/api/centro/paquetes`                         | `service.verPendientesCentro()`        |
| POST   | `/api/centro/paquetes`                         | `service.crearPaqueteManual(...)`      |
| POST   | `/api/camion/cargar`                           | `procesarSiguiente` + `cargarEnCamion` |
| POST   | `/api/camion/deshacer`                         | `service.deshacerUltimaCargaCamion()`  |
| POST   | `/api/camion/descargar`                        | `service.descargarCamion()`            |
| GET    | `/api/camion`                                  | carga actual + `pesoTotal`             |
| POST   | `/api/depositos/auditar`                       | `service.auditarDepositos(...)`        |
| GET    | `/api/depositos`                               | `service.listarDepositos()` (in-order) |
| GET    | `/api/depositos/nivel/{n}`                     | `service.depositosPorNivel(int)` (BFS) |
| GET    | `/api/depositos/{id}`                          | `service.buscarDeposito(int)`          |
| GET    | `/api/rutas`                                   | `service.listarRutas()` (sin duplicar) |
| GET    | `/api/rutas/distancia?origen=X&destino=Y`      | `service.distanciaMinimaEntreDepositos`|

Errores de dominio (`IllegalArgumentException`, `IllegalStateException`, `NoSuchElementException`) → `400` con body `{ "error": "..." }`. Cualquier otra excepcion → `500`.

## Convenciones y landmines

- **Sin dependencias externas para el main**. El unico jar en `lib/` es JUnit standalone para tests. Si alguna vez parece necesario agregar Javalin/Jackson/etc., parar y pensarlo — el plan B (`com.sun.net.httpserver` + `JsonWriter` casero) ya esta elegido por algo.
- **Complejidad en Javadoc**: cada metodo publico de los TDAs tiene `@implNote Complejidad: O(...)`. Si agregas o modificas uno, actualizalo. Es el requisito que vale 30% de la nota.
- **`List.copyOf` en `Inventario`**: los getters no hacen copia defensiva — devuelven las listas inmutables construidas una sola vez en el constructor.
- **`Deposito.conexiones` no existe**. Si te tienta agregarlo, lee antes el historial de commits.
- **`LogisticaService.depositosPorNivel` devuelve `List<Deposito>`**, no `List<String>`. El formateo de presentacion vive en la capa CLI/Web.
- **Tailwind via CDN** (`cdn.tailwindcss.com` en `index.html`). Los componentes custom (`.field`, `.btn-primary`, `.toast-*`) estan en un `<style type="text/tailwindcss">` inline porque el runtime del CDN **no procesa `@apply` en stylesheets externos**. `styles.css` solo tiene CSS plano (animaciones, scrollbars).
- **El servidor bindea a `0.0.0.0`** via `new InetSocketAddress(port)`. Es accesible desde la LAN sin configuracion extra.
- **Estaticos del front-end**: `WebServer` los sirve desde el classpath en `/web/*`. Al compilar, hay que copiar `src/main/resources/web` dentro de `out/web` (ver seccion Build), sino no los encuentra.

## Documentacion interna

- `README.md` — overview y ejecucion (para "un humano").
- `FLUJOS.md` — los 10 flujos funcionales de la aplicacion con backend/TDA involucrada y resultado esperado. Leerlo antes de cambiar la UI o un endpoint.
- `PRUEBAS.md` — resultados de la ultima batida de tests end-to-end manual con Playwright, con screenshots en `tmp/pruebas-ui/`. No es un CI — es un registro de la ultima verificacion humana.
- `Consigna.pdf` — la fuente de verdad. Cuando dudes de un requisito, abrilo.

## Testing end-to-end con Playwright MCP (modo RDP)

Los tests end-to-end de la web se corren con el plugin **Playwright MCP** y el browser **visible en el escritorio RDP** del usuario — no se usa modo headless. El punto es que el usuario pueda ver lo que el agente hace mientras testea.

- El plugin ya esta instalado. Su config vive en `~/.claude/plugins/cache/claude-plugins-official/playwright/unknown/.mcp.json` con los env vars `DISPLAY=:10.0` y `XAUTHORITY=/home/clawdio/.Xauthority` — eso enchufa Chromium al X server del xrdp. Si el browser arranca headless o no se ve, revisar ese archivo primero.
- El server se corre **en una sesion tmux** (`tmux new-session -d -s logiuade 'java -cp out ar.edu.uade.logistica.Main'`) para que sobreviva al cierre del RDP. Ver con `tmux attach -t logiuade`, matar con `tmux kill-session -t logiuade`.
- Target del server: `http://localhost:7070/` (o `http://192.168.1.101:7070/` desde otra maquina en la LAN).
- xrdp ya esta configurado para persistir al desconectar (`/etc/xrdp/sesman.ini`: `KillDisconnected=false`, `DisconnectedTimeLimit=0`, `IdleTimeLimit=0`). Cerrar el RDP no mata Chromium ni el server.
- Screenshots de las corridas van a `tmp/pruebas-ui/` — ahi se documentan los flujos probados (ver `PRUEBAS.md`).
- Las herramientas de Playwright MCP son **deferred tools**: antes de invocarlas hay que cargar el schema con `ToolSearch query="select:mcp__playwright__browser_navigate,mcp__playwright__browser_snapshot,..."`. Llamarlas sin cargar el schema da `InputValidationError`.
