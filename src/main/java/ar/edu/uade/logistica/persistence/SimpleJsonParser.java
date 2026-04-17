package ar.edu.uade.logistica.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser JSON minimalista implementado a mano con recursive descent. Soporta los tipos
 * basicos: objetos, arrays, strings (con escapes y unicode de 4 digitos hex), numeros (enteros y
 * decimales), {@code true}, {@code false} y {@code null}.
 *
 * <p>Por que parser casero en vez de Jackson/Gson: la consigna pide evitar dependencias
 * externas. El JSON del inventario es estructuralmente simple, asi que un parser recursive
 * descent de ~150 lineas alcanza. Ademas, mantener cero libs agiliza el build con
 * {@code javac} plano.
 *
 * <p>Salida: los objetos JSON se mapean a {@link LinkedHashMap} (preserva orden de insercion
 * para que los mensajes de error y los dumps sean reproducibles), los arrays a
 * {@link ArrayList}, los numeros a {@code Long} o {@code Double} segun tengan coma decimal,
 * y el resto a los tipos nativos de Java.
 */
public class SimpleJsonParser {
    private final String text;
    private int index;

    /** Constructor privado: el unico punto de entrada es {@link #parse(String)}. */
    private SimpleJsonParser(String text) {
        this.text = text;
    }

    /**
     * Parsea un documento JSON completo y valida que no haya contenido despues.
     * Usar un entry-point estatico oculta el estado mutable del cursor ({@code index})
     * y evita que el caller pueda reutilizar un parser "a medio consumir".
     */
    public static Object parse(String text) {
        SimpleJsonParser parser = new SimpleJsonParser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.index != parser.text.length()) {
            throw new IllegalArgumentException("JSON invalido: contenido extra al final.");
        }
        return value;
    }

    /**
     * Selecciona el sub-parser en funcion del primer caracter no-blanco.
     * Es el dispatch central del recursive descent: mirar un solo caracter alcanza para
     * decidir el tipo del proximo valor en JSON bien formado.
     */
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

    /**
     * Parsea un objeto {@code { "clave": valor, ... }}.
     * Usa {@link LinkedHashMap} para preservar el orden de aparicion de las claves.
     */
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

    /** Parsea un array {@code [ v1, v2, ... ]} preservando el orden. */
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

    /**
     * Parsea un string entre comillas dobles, interpretando los escapes estandar de JSON.
     * No se usa {@code String.split} ni regex para evitar sobrecosto innecesario en una
     * ruta de codigo tan caliente como el parseo de payloads.
     */
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

    /**
     * Decodifica un escape de 4 digitos hexadecimales consumiendo la secuencia y
     * devolviendo el char correspondiente.
     */
    private char parseUnicode() {
        if (index + 4 > text.length()) {
            throw new IllegalArgumentException("JSON invalido: unicode incompleto.");
        }
        String hex = text.substring(index, index + 4);
        index += 4;
        return (char) Integer.parseInt(hex, 16);
    }

    /**
     * Consume un literal ({@code true}, {@code false}, {@code null}) exacto.
     * {@code startsWith(literal, index)} evita crear substrings innecesarios.
     */
    private Object parseLiteral(String literal, Object value) {
        if (!text.startsWith(literal, index)) {
            throw new IllegalArgumentException("JSON invalido: literal inesperado.");
        }
        index += literal.length();
        return value;
    }

    /**
     * Parsea un numero: entero o decimal. La presencia del punto decide si se devuelve
     * {@code Long} o {@code Double}, lo que le ahorra un casteo al InventarioLoader al
     * elegir entre {@code getInt}/{@code getDouble}.
     *
     * <p>No soporta exponentes (1e5) porque el dataset del inventario no los usa; si
     * aparecieran, el substring lanza {@code NumberFormatException} y queda claro.
     */
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

    /**
     * Avanza whitespace y consume el caracter esperado, fallando si no esta.
     * Es el helper que da "modo estricto" al parser: cualquier desvio del formato JSON
     * aborta con mensaje explicito.
     */
    private void expect(char expected) {
        skipWhitespace();
        if (index >= text.length() || text.charAt(index) != expected) {
            throw new IllegalArgumentException("JSON invalido: se esperaba '" + expected + "'.");
        }
        index++;
    }

    /** Mira el proximo caracter no-blanco sin consumirlo. Base del look-ahead del parser. */
    private boolean peek(char expected) {
        skipWhitespace();
        return index < text.length() && text.charAt(index) == expected;
    }

    /** Avanza sobre espacios, tabs, newlines — todo lo que JSON considera blanco. */
    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }
}
