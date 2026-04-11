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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor HTTP minimalista sobre {@link HttpServer} de la JDK. Expone la API REST
 * que consume el front-end y sirve los estaticos desde classpath {@code /web}.
 * Sin dependencias externas.
 */
public class WebServer {

    private final LogisticaService service = new LogisticaService();
    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    public static void start(int port) throws IOException {
        new WebServer(port).run();
    }

    public void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/inventario/cargar", json(this::cargarInventario));
        server.createContext("/api/estado", json(this::estado));
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

        server.createContext("/", new StaticHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Logi-UADE http://localhost:" + port);
    }

    // ---------- handlers ----------

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
            m.put("inventarioCargado", pendientes + cantidadCamion + totalDepositos > 0);
            return JsonWriter.toJson(m);
        }
    }

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

    private String cargarCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            Paquete<?> p = service.procesarSiguienteEnCentro();
            service.cargarEnCamion(p);
            return JsonWriter.toJson(p);
        }
    }

    private String deshacerCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            return JsonWriter.toJson(service.deshacerUltimaCargaCamion());
        }
    }

    private String descargarCamion(HttpExchange ex) {
        requireMethod(ex, "POST");
        synchronized (service) {
            return JsonWriter.toJson(service.descargarCamion());
        }
    }

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

    private String listarRutas(HttpExchange ex) {
        requireMethod(ex, "GET");
        synchronized (service) {
            var rutas = service.listarRutas();
            return JsonWriter.toJson(Map.of("cantidad", rutas.size(), "rutas", rutas));
        }
    }

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

    @FunctionalInterface
    private interface JsonEndpoint {
        String handle(HttpExchange ex) throws Exception;
    }

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

    private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private void requireMethod(HttpExchange ex, String method) {
        if (!method.equalsIgnoreCase(ex.getRequestMethod())) {
            throw new IllegalArgumentException("Metodo no permitido: " + ex.getRequestMethod());
        }
    }

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

    private static double toDouble(Object o) {
        if (o == null) throw new IllegalArgumentException("peso requerido");
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

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

    private static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            if (path.contains("..")) {
                ex.sendResponseHeaders(400, -1);
                return;
            }
            String resource = "/web" + path;
            try (InputStream is = WebServer.class.getResourceAsStream(resource)) {
                if (is == null) {
                    byte[] notFound = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
                    ex.sendResponseHeaders(404, notFound.length);
                    ex.getResponseBody().write(notFound);
                    ex.getResponseBody().close();
                    return;
                }
                byte[] bytes = is.readAllBytes();
                ex.getResponseHeaders().set("Content-Type", contentType(path));
                ex.getResponseHeaders().set("Cache-Control", "no-store, must-revalidate");
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
                ex.getResponseBody().close();
            }
        }

        private static String contentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=utf-8";
            if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (path.endsWith(".css")) return "text/css; charset=utf-8";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
