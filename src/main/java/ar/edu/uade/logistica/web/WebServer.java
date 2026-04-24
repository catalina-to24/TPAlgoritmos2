package ar.edu.uade.logistica.web;

import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.persistence.SimpleJsonParser;
import ar.edu.uade.logistica.service.LogisticaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Servidor HTTP minimalista sobre {@link HttpServer} de la JDK. Expone la API REST
 * que consume el front-end y sirve los estaticos desde classpath {@code /web}.
 * Sin dependencias externas.
 *
 * <p>Por que {@code com.sun.net.httpserver} y no Javalin/Spark/Spring: la consigna pide
 * evitar dependencias externas. El {@code HttpServer} de la JDK no es glamoroso pero
 * alcanza para una API de ~12 endpoints. Se paga el precio de escribir a mano el routing
 * por prefijo y el manejo de errores, pero se mantiene el stack en Java puro.
 *
 * <p>Concurrencia: la JDK puede atender requests en paralelo. Cada handler sincroniza
 * sobre la misma instancia de {@link LogisticaService} con {@code synchronized (service)}
 * para que las mutaciones sobre los TDA sean serializables. No se persigue throughput:
 * este es un TPO academico, la correctitud va primero.
 */
public class WebServer {

    private final LogisticaService service = new LogisticaService();
    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    /** Entry-point estatico para que {@link ar.edu.uade.logistica.Main} arranque sin instanciar a mano. */
    public static void start(int port) throws IOException {
        new WebServer(port).run();
    }

    /**
     * Registra todos los contextos HTTP y arranca el servidor.
     *
     * <p>Orden del registro: las rutas mas especificas se registran ANTES que las generales
     * porque {@code HttpServer} resuelve por prefijo mas largo. Por eso
     * {@code /api/depositos/nivel/} y {@code /api/depositos/auditar} van antes que
     * {@code /api/depositos}, y {@code /api/rutas/distancia} antes que {@code /api/rutas}.
     *
     * <p>El bind a {@code 0.0.0.0} (implicito en {@code new InetSocketAddress(port)}) es
     * intencional: permite acceso desde otras maquinas de la LAN sin configuracion extra
     * (comentado en CLAUDE.md).
     */
    public void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/inventario/cargar", json(this::cargarInventario));
        server.createContext("/api/estado", json(this::estado));
        server.createContext("/api/centro/paquetes/demorados", json(this::paquetesDemorados));
        server.createContext("/api/centro/paquetes", json(this::centroPaquetes));
        server.createContext("/api/camion/cargar", json(this::cargarCamion));
        server.createContext("/api/camion/deshacer", json(this::deshacerCamion));
        server.createContext("/api/camion/descargar", json(this::descargarCamion));
        server.createContext("/api/camion", json(this::verCamion));
        server.createContext("/api/depositos/auditar", json(this::auditar));
        server.createContext("/api/depositos/nivel/", json(this::depositosPorNivel));
        server.createContext("/api/depositos", json(this::depositos));
        server.createContext("/api/rutas/distancia", json(this::distancia));
        server.createContext("/api/rutas", json(this::listarRutas));

        // Material de estudio: PDFs de catedra servidos desde el filesystem (clases/claseN/*.pdf).
        // Se sirven desde disco y no desde classpath porque pesan ~800 KB en total y duplicarlos
        // en el jar infla el build sin beneficio (estos archivos no cambian con el codigo).
        server.createContext("/cuestionario/pdf/clase/", new ClasePdfHandler());

        server.createContext("/", new StaticHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Logi-UADE http://localhost:" + port);
    }

    // ---------- handlers ----------

    /**
     * POST /api/inventario/cargar — recibe {@code { "path": "..." }} y carga el JSON
     * del inventario. El path default permite que la UI lo invoque sin payload.
     */
    private String cargarInventario(HttpExchange ex) throws IOException {
        requireMethod(ex, "POST");
        Map<String, Object> body = readJsonObject(ex);
        String path = (String) body.getOrDefault("path", "data/inventario.json");
        synchronized (service) {
            var inv = service.cargarInventario(Path.of(path));
            return JsonWriter.toJson(Map.of(
                    "ok", true,
                    "paquetes", inv.getPaquetes().size(),
                    "depositos", inv.getDepositos().size(),
                    "rutas", inv.getRutas().size(),
                    "path", path
            ));
        }
    }

    /**
     * GET /api/estado — KPIs para el dashboard (pendientes, carga del camion, etc).
     *
     * <p>Se usa {@link HashMap} en vez de {@link Map#of} para permitir el valor {@code null}
     * de {@code topeCamion} cuando el camion esta vacio. {@code Map.of} no acepta nulls.
     */
    private String estado(HttpExchange ex) {
        requireMethod(ex, "GET");
        synchronized (service) {
            List<Paquete<?>> carga = service.verCargaCamion();
            int pendientes = service.cantidadPendientesCentro();
            int cantidadCamion = service.cantidadPaquetesEnCamion();
            int totalDepositos = service.listarDepositos().size();
            int totalRutas = service.listarRutas().size();
            Map<String, Object> m = new HashMap<>();
            m.put("pendientesCentro", pendientes);
            m.put("cantidadCamion", cantidadCamion);
            m.put("pesoTotalCamion", service.pesoTotalCamion());
            m.put("topeCamion", carga.isEmpty() ? null : carga.get(0));
            m.put("totalDepositos", totalDepositos);
            m.put("totalRutas", totalRutas);
            // Heuristica "hay inventario cargado": alcanza con que exista al menos
            // un elemento en alguno de los tres TDA poblados por la carga.
            m.put("inventarioCargado", pendientes + cantidadCamion + totalDepositos > 0);
            return JsonWriter.toJson(m);
        }
    }

    /**
     * GET/POST /api/centro/paquetes — GET devuelve los pendientes; POST da de alta
     * un paquete manual. Se combina en un handler porque comparten recurso
     * (es el listado y la creacion del mismo agregado).
     */
    private String centroPaquetes(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            synchronized (service) {
                return JsonWriter.toJson(Map.of(
                        "cantidad", service.cantidadPendientesCentro(),
                        "paquetes", service.verPendientesCentro()
                ));
            }
        }
        if (!"POST".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Metodo no permitido: " + method);
        }
        Map<String, Object> body = readJsonObject(ex);
        String id = (String) body.get("id");
        double peso = toDouble(body.get("peso"));
        String destino = (String) body.get("destino");
        String contenido = (String) body.getOrDefault("contenido", "");
        boolean urgente = Boolean.TRUE.equals(body.get("urgente"));
        synchronized (service) {
            Paquete<String> p = service.crearPaqueteManual(id, peso, destino, contenido, urgente);
            return JsonWriter.toJson(p);
        }
    }

    /**
     * GET /api/centro/paquetes/demorados — devuelve los paquetes pendientes que llevan
     * más de 30 minutos en espera, ordenados por tiempo de permanencia decreciente.
     */
    private String paquetesDemorados(HttpExchange ex) {
        requireMethod(ex, "GET");
        synchronized (service) {
            return JsonWriter.toJson(Map.of(
                    "cantidad", service.verPaquetesDemorados().size(),
                    "paquetes", service.verPaquetesDemorados()
            ));
        }
    }

    /**
     * POST /api/camion/cargar — flujo combinado: saca del centro y apila en el camion.
     * Se hace en el server y no en el cliente para garantizar atomicidad sobre el lock.
     */
    private String cargarCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            Paquete<?> p = service.procesarSiguienteEnCentro();
            service.cargarEnCamion(p);
            return JsonWriter.toJson(p);
        }
    }

    /** POST /api/camion/deshacer — pop de la pila del camion. */
    private String deshacerCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            return JsonWriter.toJson(service.deshacerUltimaCargaCamion());
        }
    }

    /** POST /api/camion/descargar — pop de la pila del camion (entrega en destino). */
    private String descargarCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            return JsonWriter.toJson(service.descargarCamion());
        }
    }

    /** GET /api/camion — vista de la carga actual con totales. */
    private String verCamion(HttpExchange ex) {
        requireMethod(ex, "GET");
        synchronized (service) {
            return JsonWriter.toJson(Map.of(
                    "cantidad", service.cantidadPaquetesEnCamion(),
                    "pesoTotal", service.pesoTotalCamion(),
                    "paquetes", service.verCargaCamion()
            ));
        }
    }

    /**
     * POST /api/depositos/auditar — corre la auditoria post-orden.
     *
     * <p>El body es opcional: si no viene {@code fechaReferencia} se usa {@code now()}.
     * Esto permite desde la UI simular auditorias hacia el pasado (util para demos).
     */
    private String auditar(HttpExchange ex) throws IOException {
        requireMethod(ex, "POST");
        Map<String, Object> body = ex.getRequestBody().available() > 0 ? readJsonObject(ex) : Map.of();
        LocalDateTime fecha = body.get("fechaReferencia") instanceof String s && !s.isBlank()
                ? LocalDateTime.parse(s)
                : LocalDateTime.now();
        synchronized (service) {
            List<Integer> ids = service.auditarDepositos(fecha);
            return JsonWriter.toJson(Map.of(
                    "fechaReferencia", fecha.toString(),
                    "idsVisitados", ids
            ));
        }
    }

    /**
     * GET /api/depositos/nivel/{n} — reporte BFS por nivel.
     * Se extrae {@code n} del path manualmente porque el {@code HttpServer} de la JDK
     * no tiene routing por parametros.
     */
    private String depositosPorNivel(HttpExchange ex) {
        requireMethod(ex, "GET");
        String path = ex.getRequestURI().getPath();
        String suffix = path.substring("/api/depositos/nivel/".length());
        int nivel = Integer.parseInt(suffix);
        synchronized (service) {
            return JsonWriter.toJson(Map.of(
                    "nivel", nivel,
                    "depositos", service.depositosPorNivel(nivel)
            ));
        }
    }

    /**
     * GET /api/depositos[/{id}] — listado general o busqueda puntual.
     *
     * <p>Se combinan en un handler porque comparten prefijo y ambos son GET. La logica
     * de ramificacion distingue por presencia y forma del sufijo: sin sufijo lista,
     * con {@code /N} busca. Si el sufijo tiene otra forma, se rechaza con 400.
     */
    private String depositos(HttpExchange ex) {
        requireMethod(ex, "GET");
        String path = ex.getRequestURI().getPath();
        String suffix = path.length() > "/api/depositos".length()
                ? path.substring("/api/depositos".length())
                : "";
        if (suffix.isEmpty() || suffix.equals("/")) {
            synchronized (service) {
                var lista = service.listarDepositos();
                return JsonWriter.toJson(Map.of("cantidad", lista.size(), "depositos", lista));
            }
        }
        if (!suffix.startsWith("/") || suffix.substring(1).contains("/")) {
            throw new IllegalArgumentException("Ruta invalida.");
        }
        int id = Integer.parseInt(suffix.substring(1));
        synchronized (service) {
            return JsonWriter.toJson(service.buscarDeposito(id));
        }
    }

    /** GET /api/rutas — listado sin duplicar aristas. */
    private String listarRutas(HttpExchange ex) {
        requireMethod(ex, "GET");
        synchronized (service) {
            var rutas = service.listarRutas();
            return JsonWriter.toJson(Map.of("cantidad", rutas.size(), "rutas", rutas));
        }
    }

    /** GET /api/rutas/distancia?origen=X&destino=Y — Dijkstra sobre el grafo. */
    private String distancia(HttpExchange ex) {
        requireMethod(ex, "GET");
        Map<String, String> q = parseQuery(ex.getRequestURI());
        int origen = Integer.parseInt(q.get("origen"));
        int destino = Integer.parseInt(q.get("destino"));
        synchronized (service) {
            int km = service.distanciaMinimaEntreDepositos(origen, destino);
            return JsonWriter.toJson(Map.of("origen", origen, "destino", destino, "distanciaKm", km));
        }
    }

    // ---------- helpers ----------

    /** Interfaz funcional local para poder lanzar excepciones checked desde los handlers. */
    @FunctionalInterface
    private interface JsonEndpoint {
        String handle(HttpExchange ex) throws Exception;
    }

    /**
     * Adapta un {@link JsonEndpoint} a {@link HttpHandler}, centralizando el manejo de
     * errores: las excepciones de dominio ({@link IllegalArgumentException},
     * {@link IllegalStateException}, {@link java.util.NoSuchElementException}) se
     * traducen a 400 (error del cliente / estado invalido); cualquier otra excepcion
     * es un bug y se devuelve como 500 con stacktrace a consola.
     */
    private HttpHandler json(JsonEndpoint endpoint) {
        return exchange -> {
            try {
                String body = endpoint.handle(exchange);
                sendJson(exchange, 200, body);
            } catch (IllegalArgumentException | IllegalStateException | java.util.NoSuchElementException e) {
                sendJson(exchange, 400, JsonWriter.error(e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, JsonWriter.error(e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        };
    }

    /** Escribe la respuesta como JSON UTF-8 con el status indicado. */
    private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    /** Valida que el metodo HTTP sea el esperado. Si no, lanza 400 via {@link #json}. */
    private void requireMethod(HttpExchange ex, String method) {
        if (!method.equalsIgnoreCase(ex.getRequestMethod())) {
            throw new IllegalArgumentException("Metodo no permitido: " + ex.getRequestMethod());
        }
    }

    /**
     * Lee el body como JSON y lo exige objeto. Devuelve {@link Map#of()} vacio si no hay
     * body para que los handlers tolerantes (p.ej. auditar) no tengan que distinguir
     * "sin body" de "body vacio".
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(HttpExchange ex) throws IOException {
        byte[] raw = ex.getRequestBody().readAllBytes();
        if (raw.length == 0) {
            return Map.of();
        }
        Object parsed = SimpleJsonParser.parse(new String(raw, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON.");
        }
        return (Map<String, Object>) m;
    }

    /** Coerce a {@code double}: acepta {@link Number} (lo habitual del parser) o string. */
    private static double toDouble(Object o) {
        if (o == null) throw new IllegalArgumentException("peso requerido");
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    /**
     * Parseo manual de query string. No decodifica {@code %XX} porque el dominio no usa
     * valores con caracteres raros en query (solo ints); si alguna vez hace falta, cambiar
     * por {@link java.net.URLDecoder}.
     */
    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isBlank()) return result;
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) result.put(pair, "");
            else result.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return result;
    }

    // ---------- estaticos ----------

    /**
     * Handler que sirve archivos del front-end desde classpath ({@code /web/*}).
     *
     * <p>Por que desde classpath y no desde filesystem: al compilar se copia
     * {@code src/main/resources/web} a {@code out/web} y los estaticos viajan con las
     * clases. Esto permite distribuir el jar sin depender de rutas absolutas.
     *
     * <p>Se bloquea {@code ..} en el path como medida minima contra path traversal: el
     * endpoint es publico por LAN, asi que aun siendo un TPO, mejor no servir {@code ../etc/passwd}.
     */
    private static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            // Aliases para URLs limpias:
            //   /                          -> /index.html (SPA del TPO)
            //   /cuestionario              -> /cuestionario/index.html (hub)
            //   /cuestionario/quiz         -> /cuestionario/quiz.html
            //   /cuestionario/resumenes    -> /cuestionario/resumenes.html
            //   /cuestionario/clase/{1-6}  -> /cuestionario/clase{n}.html
            //   /cuestionario.html (legacy) -> /cuestionario (ruta nueva)
            if (path.equals("/")) {
                path = "/index.html";
            } else if (path.equals("/cuestionario.html")) {
                ex.getResponseHeaders().set("Location", "/cuestionario");
                ex.sendResponseHeaders(302, -1);
                return;
            } else if (path.equals("/cuestionario") || path.equals("/cuestionario/")) {
                path = "/cuestionario/index.html";
            } else if (path.equals("/cuestionario/quiz")) {
                path = "/cuestionario/quiz.html";
            } else if (path.equals("/cuestionario/resumenes")) {
                path = "/cuestionario/resumenes.html";
            } else if (path.equals("/cuestionario/exposicion") || path.equals("/cuestionario/exposicion/")) {
                path = "/cuestionario/exposicion/index.html";
            } else if (path.equals("/cuestionario/exposicion/arquitectura")) {
                path = "/cuestionario/exposicion/arquitectura.html";
            } else if (path.equals("/cuestionario/exposicion/teorica")) {
                path = "/cuestionario/exposicion/teorica.html";
            } else if (path.startsWith("/cuestionario/clase/")) {
                String suffix = path.substring("/cuestionario/clase/".length());
                // Validacion estricta: solo digitos, 1..6. Cualquier otra cosa es 404.
                if (!suffix.matches("[1-6]")) {
                    notFound(ex, path);
                    return;
                }
                path = "/cuestionario/clase" + suffix + ".html";
            }
            if (path.contains("..")) {
                ex.sendResponseHeaders(400, -1);
                return;
            }
            String resource = "/web" + path;
            try (InputStream is = WebServer.class.getResourceAsStream(resource)) {
                if (is == null) {
                    notFound(ex, path);
                    return;
                }
                byte[] bytes = is.readAllBytes();
                ex.getResponseHeaders().set("Content-Type", contentType(path));
                // no-store para que durante el desarrollo el navegador no quede pegado
                // a una version vieja del JS/HTML tras un rebuild.
                ex.getResponseHeaders().set("Cache-Control", "no-store, must-revalidate");
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
                ex.getResponseBody().close();
            }
        }

        /** Mapeo extension -> Content-Type. Lo suficiente para los tipos que sirve la UI. */
        private static String contentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (path.endsWith(".css")) return "text/css; charset=utf-8";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".ico")) return "image/x-icon";
            if (path.endsWith(".pdf")) return "application/pdf";
            return "application/octet-stream";
        }
    }

    /** 404 compartido entre los handlers de estaticos. */
    private static void notFound(HttpExchange ex, String path) throws IOException {
        byte[] body = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(404, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    /**
     * Sirve los PDFs de catedra ({@code clases/clase{n}/*.pdf}) bajo
     * {@code /cuestionario/pdf/clase/{n}}.
     *
     * <p>Se sirve desde filesystem (y no desde classpath) para no duplicar ~800 KB en
     * el jar. Las clases solo se despliegan en desarrollo; en un build que no incluya
     * la carpeta {@code clases/}, estos endpoints devuelven 404 limpiamente.
     *
     * <p>Seguridad: {@code n} se valida como digito 1..6 contra regex. No se toma nada
     * del path ni del body para construir la ruta al disco, solo el digito.
     */
    private static class ClasePdfHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            String suffix = path.substring("/cuestionario/pdf/clase/".length());
            if (!suffix.matches("[1-6]")) {
                notFound(ex, path);
                return;
            }
            // La carpeta "natural" es clases/clase{n}/. Pero clase6 fue dictada junto con
            // clase5 y su PDF vive en clases/clase5/. Por eso buscamos primero en el
            // directorio homonimo y, si no hay match, recorremos toda la carpeta clases/
            // en busca de un PDF con "Clase{n}" en el nombre.
            Path pdf = findClasePdf(Path.of("clases", "clase" + suffix), suffix);
            if (pdf == null) pdf = findClasePdfInRoot(Path.of("clases"), suffix);
            if (pdf == null) {
                notFound(ex, path);
                return;
            }
            byte[] bytes = Files.readAllBytes(pdf);
            ex.getResponseHeaders().set("Content-Type", "application/pdf");
            ex.getResponseHeaders().set("Content-Disposition",
                    "inline; filename=\"AyED-II-clase-" + suffix + ".pdf\"");
            ex.getResponseHeaders().set("Cache-Control", "no-store, must-revalidate");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }

        /**
         * Busca el primer .pdf con "Clase{n}" o "Clase {n}" en el nombre dentro de {@code dir}.
         * El espacio opcional contempla que el PDF de clase 1 se llama "Clase 1" y los demas
         * "ClaseN" pegado.
         */
        private static Path findClasePdf(Path dir, String n) throws IOException {
            if (!Files.isDirectory(dir)) return null;
            // Regex: ".*Clase ?N(?=[^0-9]|$).*\.pdf" — el lookahead evita que "Clase1" matchee al buscar "Clase 1" e inverso.
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    ".*Clase ?" + n + "(?=[^0-9]|$).*\\.pdf$",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            try (Stream<Path> s = Files.list(dir)) {
                return s.filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                        .sorted()
                        .findFirst()
                        .orElse(null);
            }
        }

        /** Fallback: recorre todos los subdirectorios de clases/ buscando el PDF. */
        private static Path findClasePdfInRoot(Path root, String n) throws IOException {
            if (!Files.isDirectory(root)) return null;
            try (Stream<Path> dirs = Files.list(root)) {
                for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                    Path found = findClasePdf(dir, n);
                    if (found != null) return found;
                }
            }
            return null;
        }
    }
}
