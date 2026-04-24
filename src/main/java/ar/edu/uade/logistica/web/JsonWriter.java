package ar.edu.uade.logistica.web;

import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.model.Ruta;

import java.util.List;
import java.util.Map;

/**
 * Serializador JSON minimalista. Suficiente para los payloads del proyecto y simetrico
 * con {@link ar.edu.uade.logistica.persistence.SimpleJsonParser}. Evita traer Jackson.
 *
 * <p>Por que casero: la consigna prohibe dependencias externas y los tipos que se
 * serializan son acotados (tres entidades del dominio + {@link Map}/{@link List}/primitivos).
 * Un dispatcher por {@code instanceof} con pattern matching alcanza y se mantiene en un
 * unico archivo facil de auditar.
 *
 * <p>Diseno: clase utilitaria {@code final} con constructor privado para bloquear
 * instanciacion (no tiene estado, todo es estatico).
 */
public final class JsonWriter {

    private JsonWriter() {}

    /** Entry-point publico: serializa cualquier valor soportado a string JSON. */
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    /** Builder para respuestas de exito genericas. */
    public static String ok(String message) {
        return toJson(Map.of("ok", true, "message", message));
    }

    /** Builder para respuestas de error genericas; lo usa el WebServer en 400/500. */
    public static String error(String message) {
        return toJson(Map.of("ok", false, "error", message));
    }

    /**
     * Dispatcher por tipo. Se evita reflection / anotaciones (lo que haria Jackson) y
     * se arma el switch a mano. Trade-off consciente: agregar un tipo de dominio nuevo
     * requiere tocar este archivo, pero el alcance del TPO lo hace viable.
     */
    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean b) {
            sb.append(b.toString());
        } else if (value instanceof Number n) {
            sb.append(n.toString());
        } else if (value instanceof Paquete<?> p) {
            writePaquete(sb, p);
        } else if (value instanceof Deposito d) {
            writeDeposito(sb, d);
        } else if (value instanceof Ruta r) {
            // Inline para Ruta porque es un record trivial: 3 ints y ningun string.
            sb.append("{\"origen\":").append(r.origen())
              .append(",\"destino\":").append(r.destino())
              .append(",\"distanciaKm\":").append(r.distanciaKm())
              .append('}');
        } else if (value instanceof Map<?, ?> m) {
            writeMap(sb, m);
        } else if (value instanceof List<?> l) {
            writeList(sb, l);
        } else if (value instanceof int[] arr) {
            sb.append('[');
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(arr[i]);
            }
            sb.append(']');
        } else {
            // Fallback: delega a toString() y lo serializa como string JSON. Evita que
            // un tipo no soportado rompa el endpoint silenciosamente.
            writeString(sb, value.toString());
        }
    }

    /** Serializa un mapa como objeto JSON preservando el orden de las entradas. */
    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            write(sb, e.getValue());
        }
        sb.append('}');
    }

    /** Serializa una lista como array JSON. */
    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            write(sb, list.get(i));
        }
        sb.append(']');
    }

    /**
     * Representacion explicita de {@link Paquete}: emite ademas un campo {@code prioritario}
     * calculado en el momento, para que la UI no tenga que replicar la regla de
     * "urgente || peso > 50kg".
     */
    private static void writePaquete(StringBuilder sb, Paquete<?> p) {
        sb.append('{');
        writeString(sb, "id"); sb.append(':'); writeString(sb, p.getId()); sb.append(',');
        writeString(sb, "peso"); sb.append(':').append(p.getPeso()).append(',');
        writeString(sb, "destino"); sb.append(':'); writeString(sb, p.getDestino()); sb.append(',');
        writeString(sb, "contenido"); sb.append(':');
        Object contenido = p.getContenido();
        if (contenido == null) {
            sb.append("null");
        } else {
            writeString(sb, contenido.toString());
        }
        sb.append(',');
        writeString(sb, "urgente"); sb.append(':').append(p.isUrgente()).append(',');
        writeString(sb, "minutosIngreso"); sb.append(':').append(p.getMinutosIngreso()).append(',');
        writeString(sb, "prioritario"); sb.append(':').append(p.requierePrioridad());
        sb.append('}');
    }

    /**
     * Representacion explicita de {@link Deposito}. La fecha se serializa como string
     * ISO-8601 (lo que emite {@code LocalDateTime.toString()}), que es lo que consume
     * el parser simetrico al re-cargar.
     */
    private static void writeDeposito(StringBuilder sb, Deposito d) {
        sb.append('{');
        writeString(sb, "id"); sb.append(':').append(d.getId()).append(',');
        writeString(sb, "nombre"); sb.append(':'); writeString(sb, d.getNombre()); sb.append(',');
        writeString(sb, "visitado"); sb.append(':').append(d.isVisitado()).append(',');
        writeString(sb, "fechaUltimaAuditoria"); sb.append(':');
        if (d.getFechaUltimaAuditoria() == null) {
            sb.append("null");
        } else {
            writeString(sb, d.getFechaUltimaAuditoria().toString());
        }
        sb.append('}');
    }

    /**
     * Escribe un string JSON con escapes correctos. Los chars de control (&lt; 0x20) se
     * emiten como {@code \\uXXXX} para que el output sea siempre JSON valido aunque el
     * origen tenga caracteres raros.
     */
    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
