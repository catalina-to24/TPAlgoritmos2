package ar.edu.uade.logistica.persistence;

import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Inventario;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.model.Ruta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador entre el JSON crudo (parseado por {@link SimpleJsonParser}) y las entidades
 * del dominio ({@link Paquete}, {@link Deposito}, {@link Ruta}).
 *
 * <p>Por que separado del parser: el parser solo entiende sintaxis JSON; este loader
 * entiende el contrato del inventario ({@code paquetes}, {@code depositos}, {@code rutas})
 * y valida tipos campo por campo. Mantenerlos separados facilita los tests y reutilizar
 * el parser en otros contextos (p.ej. {@code WebServer.readJsonObject}).
 */
public class InventarioLoader {

    /**
     * Lee el archivo indicado, lo parsea como JSON y construye el {@link Inventario}.
     * Falla con {@link IllegalArgumentException} si la raiz no es un objeto o si falta
     * algun campo requerido.
     */
    public Inventario cargar(Path path) throws IOException {
        String json = Files.readString(path);
        Object parsed = SimpleJsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("El JSON raiz debe ser un objeto.");
        }

        List<Paquete<String>> paquetes = cargarPaquetes(root.get("paquetes"));
        List<Deposito> depositos = cargarDepositos(root.get("depositos"));
        List<Ruta> rutas = cargarRutas(root.get("rutas"));
        return new Inventario(paquetes, depositos, rutas);
    }

    /**
     * Convierte el array JSON de paquetes en entidades tipadas. Si el valor no es una
     * lista se devuelve una lista vacia — tolerar un JSON sin la seccion permite datasets
     * parciales (p.ej. solo depositos y rutas).
     */
    private List<Paquete<String>> cargarPaquetes(Object value) {
        List<Paquete<String>> paquetes = new ArrayList<>();
        if (!(value instanceof List<?> items)) {
            return paquetes;
        }
        for (Object item : items) {
            Map<?, ?> paqueteMap = asMap(item, "Cada paquete debe ser un objeto JSON.");
            paquetes.add(new Paquete<>(
                    getString(paqueteMap, "id"),
                    getDouble(paqueteMap, "peso"),
                    getString(paqueteMap, "destino"),
                    getString(paqueteMap, "contenido"),
                    getBoolean(paqueteMap, "urgente")
            ));
        }
        return paquetes;
    }

    /** Idem {@link #cargarPaquetes(Object)} pero para depositos. */
    private List<Deposito> cargarDepositos(Object value) {
        List<Deposito> depositos = new ArrayList<>();
        if (!(value instanceof List<?> items)) {
            return depositos;
        }
        for (Object item : items) {
            Map<?, ?> depositoMap = asMap(item, "Cada deposito debe ser un objeto JSON.");
            depositos.add(new Deposito(
                    getInt(depositoMap, "id"),
                    getString(depositoMap, "nombre"),
                    getBoolean(depositoMap, "visitado"),
                    getNullableDateTime(depositoMap.get("fechaUltimaAuditoria"))
            ));
        }
        return depositos;
    }

    /** Idem pero para rutas; delega a {@link Ruta} la validacion de distancia positiva. */
    private List<Ruta> cargarRutas(Object value) {
        List<Ruta> rutas = new ArrayList<>();
        if (!(value instanceof List<?> items)) {
            return rutas;
        }
        for (Object item : items) {
            Map<?, ?> rutaMap = asMap(item, "Cada ruta debe ser un objeto JSON.");
            rutas.add(new Ruta(
                    getInt(rutaMap, "origen"),
                    getInt(rutaMap, "destino"),
                    getInt(rutaMap, "distanciaKm")
            ));
        }
        return rutas;
    }

    /** Type-check defensivo: si el parser devolvio algo que no es mapa, falla con mensaje claro. */
    private Map<?, ?> asMap(Object value, String message) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /** Helper tipado para strings requeridos. Centraliza el mensaje de error. */
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser un texto.");
        }
        return text;
    }

    /** Helper tipado para booleanos requeridos. */
    private boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser booleano.");
        }
        return bool;
    }

    /**
     * Helper para enteros. Acepta cualquier {@link Number} porque el parser puede emitir
     * {@code Long} (enteros sin decimal) y queremos tolerar igualmente un {@code Double}
     * "entero" (p.ej. 3.0) sin falso positivo.
     */
    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser numerico.");
        }
        return number.intValue();
    }

    /** Helper para decimales; mismo criterio que {@link #getInt}. */
    private double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser numerico.");
        }
        return number.doubleValue();
    }

    /**
     * Convierte una fecha ISO-8601 a {@link LocalDateTime}. Acepta {@code null} porque
     * {@code fechaUltimaAuditoria} puede no estar seteada cuando el deposito nunca fue
     * auditado.
     */
    private LocalDateTime getNullableDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("La fecha de auditoria debe ser un texto ISO-8601.");
        }
        return LocalDateTime.parse(text);
    }
}
