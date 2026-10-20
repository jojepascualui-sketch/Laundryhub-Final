package com.laundryhub.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A JSON array with typed access.
 */
public final class JsonArray {

    private final List<Object> items;

    /** Creates an empty array. */
    public JsonArray() {
        this(new ArrayList<>());
    }

    JsonArray(List<Object> items) {
        this.items = items;
    }

    /**
     * @param value value to append
     * @return this array, for chaining
     */
    public JsonArray add(Object value) {
        items.add(value);
        return this;
    }

    /** @return number of items */
    public int size() {
        return items.size();
    }

    /** @return true when there are no items */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * @param index position
     * @return the raw item
     */
    public Object get(int index) {
        return items.get(index);
    }

    /**
     * @param index position
     * @return the item as an object, or null when it is not one
     */
    public JsonObject getObject(int index) {
        Object v = items.get(index);
        return v instanceof JsonObject ? (JsonObject) v : null;
    }

    /** @return every item that is a JSON object, in order */
    public List<JsonObject> objects() {
        List<JsonObject> result = new ArrayList<>();
        for (Object v : items) {
            if (v instanceof JsonObject) {
                result.add((JsonObject) v);
            }
        }
        return result;
    }

    /** @return read-only view of the items */
    public List<Object> asList() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public String toString() {
        return Json.stringify(this);
    }
}
