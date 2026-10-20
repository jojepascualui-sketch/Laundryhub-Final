package com.laundryhub.util;

import com.laundryhub.exception.LaundryHubException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON parser and writer in plain Java (no browser APIs), used for every
 * Supabase request and response.
 *
 * <p>Parsed values are {@link JsonObject}, {@link JsonArray}, {@link String},
 * {@link Double}, {@link Boolean} or {@code null}.
 */
public final class Json {

    private static final int HEX_RADIX = 16;
    private static final int UNICODE_ESCAPE_LENGTH = 4;
    private static final char FIRST_PRINTABLE = 0x20;

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Parses any JSON value.
     *
     * @param text JSON text
     * @return the parsed value
     * @throws LaundryHubException when the text is not valid JSON
     */
    public static Object parse(String text) {
        if (text == null) {
            throw new LaundryHubException("Empty JSON");
        }
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected text after JSON value");
        }
        return value;
    }

    /**
     * Parses a JSON object.
     *
     * @param text JSON text
     * @return the object
     */
    public static JsonObject parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof JsonObject)) {
            throw new LaundryHubException("Expected a JSON object");
        }
        return (JsonObject) value;
    }

    /**
     * Parses a JSON array.
     *
     * @param text JSON text
     * @return the array
     */
    public static JsonArray parseArray(String text) {
        Object value = parse(text);
        if (!(value instanceof JsonArray)) {
            throw new LaundryHubException("Expected a JSON array");
        }
        return (JsonArray) value;
    }

    /**
     * Writes a value as compact JSON.
     *
     * @param value a JsonObject, JsonArray, Map, List, String, Number, Boolean or null
     * @return JSON text
     */
    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    // ---- writer -------------------------------------------------------------

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value).booleanValue() ? "true" : "false");
        } else if (value instanceof Double || value instanceof Float) {
            writeDouble(sb, ((Number) value).doubleValue());
        } else if (value instanceof Number) {
            sb.append(((Number) value).longValue());
        } else if (value instanceof JsonObject) {
            writeMap(sb, ((JsonObject) value).asMap());
        } else if (value instanceof JsonArray) {
            writeList(sb, ((JsonArray) value).asList());
        } else if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeList(sb, (List<?>) value);
        } else if (value instanceof Enum) {
            writeString(sb, ((Enum<?>) value).name());
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeDouble(StringBuilder sb, double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            sb.append("null");
        } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            sb.append((long) d);
        } else {
            sb.append(d);
        }
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
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
            if (i > 0) {
                sb.append(',');
            }
            write(sb, list.get(i));
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < FIRST_PRINTABLE) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int p = hex.length(); p < UNICODE_ESCAPE_LENGTH; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ---- parser -------------------------------------------------------------

    private Object readValue() {
        if (pos >= text.length()) {
            throw error("Unexpected end of JSON");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': expectWord("true"); return Boolean.TRUE;
            case 'f': expectWord("false"); return Boolean.FALSE;
            case 'n': expectWord("null"); return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return readNumber();
                }
                throw error("Unexpected character '" + c + "'");
        }
    }

    private JsonObject readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // {
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return new JsonObject(map);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected a property name");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return new JsonObject(map);
            }
            if (c != ',') {
                throw error("Expected ',' or '}'");
            }
        }
    }

    private JsonArray readArray() {
        List<Object> list = new ArrayList<>();
        pos++; // [
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return new JsonArray(list);
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return new JsonArray(list);
            }
            if (c != ',') {
                throw error("Expected ',' or ']'");
            }
        }
    }

    private String readString() {
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = next();
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (pos + UNICODE_ESCAPE_LENGTH > text.length()) {
                        throw error("Bad unicode escape");
                    }
                    sb.append((char) Integer.parseInt(text.substring(pos, pos + UNICODE_ESCAPE_LENGTH), HEX_RADIX));
                    pos += UNICODE_ESCAPE_LENGTH;
                    break;
                default:
                    throw error("Bad escape \\" + e);
            }
        }
    }

    private Double readNumber() {
        int start = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }
        try {
            return Double.valueOf(Double.parseDouble(text.substring(start, pos)));
        } catch (NumberFormatException ex) {
            throw error("Bad number");
        }
    }

    private void expectWord(String word) {
        if (!text.startsWith(word, pos)) {
            throw error("Expected " + word);
        }
        pos += word.length();
    }

    private void expect(char c) {
        if (next() != c) {
            throw error("Expected '" + c + "'");
        }
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private char next() {
        if (pos >= text.length()) {
            throw error("Unexpected end of JSON");
        }
        return text.charAt(pos++);
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }

    private LaundryHubException error(String message) {
        return new LaundryHubException(message + " at position " + pos);
    }
}
