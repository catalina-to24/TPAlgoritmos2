package ar.edu.uade.logistica.web;

import ar.edu.uade.logistica.model.Deposito;
import ar.edu.uade.logistica.model.Paquete;
import ar.edu.uade.logistica.model.Ruta;

import java.util.List;
import java.util.Map;

/**
 * Serializador JSON minimalista. Suficiente para los payloads del proyecto y simetrico
 * con {@link ar.edu.uade.logistica.persistence.SimpleJsonParser}. Evita traer Jackson.
 */
public final class JsonWriter {

    private JsonWriter() {}

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    public static String ok(String message) {
        return toJson(Map.of("ok", true, "message", message));
    }

    public static String error(String message) {
        return toJson(Map.of("ok", false, "error", message));
    }

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
            writeString(sb, value.toString());
        }
    }

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

    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            write(sb, list.get(i));
        }
        sb.append(']');
    }

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
        writeString(sb, "prioritario"); sb.append(':').append(p.requierePrioridad());
        sb.append('}');
    }

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
