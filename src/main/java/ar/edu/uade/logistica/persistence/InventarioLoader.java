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

public class InventarioLoader {
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

    private Map<?, ?> asMap(Object value, String message) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser un texto.");
        }
        return text;
    }

    private boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Boolean bool)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser booleano.");
        }
        return bool;
    }

    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser numerico.");
        }
        return number.intValue();
    }

    private double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("El campo '" + key + "' debe ser numerico.");
        }
        return number.doubleValue();
    }

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
