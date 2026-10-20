package com.laundryhub.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A JSON object with typed getters. Missing keys and JSON {@code null} return the
 * given default (or null), so mapping database rows never throws for optional columns.
 */
public final class JsonObject {

    // OOP: Encapsulation — the map is private; callers use typed getters and put().
    private final Map<String, Object> values;

    /** Creates an empty object. */
    public JsonObject() {
        this(new LinkedHashMap<>());
    }

    JsonObject(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * Adds or replaces a value.
     *
     * @param key   property name
     * @param value String, Number, Boolean, JsonObject, JsonArray, enum or null
     * @return this object, for chaining
     */
    public JsonObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    /**
     * @param key property name
     * @return true when the key exists (even if its value is null)
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * @param key property name
     * @return true when the key is missing or JSON null
     */
    public boolean isNull(String key) {
        return values.get(key) == null;
    }

    /**
     * @param key property name
     * @return the raw value, or null
     */
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * @param key property name
     * @return the value as text, or null when missing
     */
    public String getString(String key) {
        Object v = values.get(key);
        return v == null ? null : (v instanceof Double ? Json.stringify(v) : v.toString());
    }

    /**
     * @param key      property name
     * @param fallback value when missing
     * @return the value as text
     */
    public String getString(String key, String fallback) {
        String v = getString(key);
        return v == null ? fallback : v;
    }

    /**
     * @param key      property name
     * @param fallback value when missing
     * @return the value as a double
     */
    public double getDouble(String key, double fallback) {
        Object v = values.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    /**
     * @param key property name
     * @return the value as a Double, or null when missing
     */
    public Double getDoubleOrNull(String key) {
        return isNull(key) ? null : Double.valueOf(getDouble(key, 0));
    }

    /**
     * @param key      property name
     * @param fallback value when missing
     * @return the value as a long (rounded)
     */
    public long getLong(String key, long fallback) {
        return isNull(key) ? fallback : Math.round(getDouble(key, fallback));
    }

    /**
     * @param key      property name
     * @param fallback value when missing
     * @return the value as an int (rounded)
     */
    public int getInt(String key, int fallback) {
        return (int) getLong(key, fallback);
    }

    /**
     * Reads a peso amount (e.g. {@code 410.00}) as centavos ({@code 41000}).
     *
     * @param key property name
     * @return centavos, 0 when missing
     */
    public long getCentavos(String key) {
        return MoneyFormatter.toCentavos(getDouble(key, 0));
    }

    /**
     * @param key      property name
     * @param fallback value when missing
     * @return the value as a boolean
     */
    public boolean getBoolean(String key, boolean fallback) {
        Object v = values.get(key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : fallback;
    }

    /**
     * @param key property name
     * @return the nested object, or null
     */
    public JsonObject getObject(String key) {
        Object v = values.get(key);
        return v instanceof JsonObject ? (JsonObject) v : null;
    }

    /**
     * @param key property name
     * @return the nested array, or an empty array when missing
     */
    public JsonArray getArray(String key) {
        Object v = values.get(key);
        return v instanceof JsonArray ? (JsonArray) v : new JsonArray();
    }

    /** @return property names in order */
    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /** @return read-only view of the values */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return Json.stringify(this);
    }
}
