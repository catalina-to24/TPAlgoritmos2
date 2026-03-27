package ar.edu.uade.logistica.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimpleJsonParser {
    private final String text;
    private int index;

    private SimpleJsonParser(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        SimpleJsonParser parser = new SimpleJsonParser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.index != parser.text.length()) {
            throw new IllegalArgumentException("JSON invalido: contenido extra al final.");
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (index >= text.length()) {
            throw new IllegalArgumentException("JSON invalido: fin inesperado.");
        }
        char current = text.charAt(index);
        return switch (current) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseLiteral("true", Boolean.TRUE);
            case 'f' -> parseLiteral("false", Boolean.FALSE);
            case 'n' -> parseLiteral("null", null);
            default -> {
                if (current == '-' || Character.isDigit(current)) {
                    yield parseNumber();
                }
                throw new IllegalArgumentException("JSON invalido: caracter inesperado '" + current + "'.");
            }
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        skipWhitespace();
        Map<String, Object> object = new LinkedHashMap<>();
        if (peek('}')) {
            expect('}');
            return object;
        }
        while (true) {
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            object.put(key, value);
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        expect('[');
        skipWhitespace();
        List<Object> array = new ArrayList<>();
        if (peek(']')) {
            expect(']');
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return array;
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char current = text.charAt(index++);
            if (current == '"') {
                return builder.toString();
            }
            if (current == '\\') {
                if (index >= text.length()) {
                    throw new IllegalArgumentException("JSON invalido: escape incompleto.");
                }
                char escaped = text.charAt(index++);
                builder.append(switch (escaped) {
                    case '"', '\\', '/' -> escaped;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> parseUnicode();
                    default -> throw new IllegalArgumentException("JSON invalido: escape no soportado.");
                });
            } else {
                builder.append(current);
            }
        }
        throw new IllegalArgumentException("JSON invalido: string sin cerrar.");
    }

    private char parseUnicode() {
        if (index + 4 > text.length()) {
            throw new IllegalArgumentException("JSON invalido: unicode incompleto.");
        }
        String hex = text.substring(index, index + 4);
        index += 4;
        return (char) Integer.parseInt(hex, 16);
    }

    private Object parseLiteral(String literal, Object value) {
        if (!text.startsWith(literal, index)) {
            throw new IllegalArgumentException("JSON invalido: literal inesperado.");
        }
        index += literal.length();
        return value;
    }

    private Number parseNumber() {
        int start = index;
        if (text.charAt(index) == '-') {
            index++;
        }
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        if (index < text.length() && text.charAt(index) == '.') {
            index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            return Double.parseDouble(text.substring(start, index));
        }
        return Long.parseLong(text.substring(start, index));
    }

    private void expect(char expected) {
        skipWhitespace();
        if (index >= text.length() || text.charAt(index) != expected) {
            throw new IllegalArgumentException("JSON invalido: se esperaba '" + expected + "'.");
        }
        index++;
    }

    private boolean peek(char expected) {
        skipWhitespace();
        return index < text.length() && text.charAt(index) == expected;
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }
}
