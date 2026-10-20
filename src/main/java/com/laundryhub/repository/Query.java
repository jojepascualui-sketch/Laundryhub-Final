package com.laundryhub.repository;

import com.laundryhub.util.UrlEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds PostgREST query strings, e.g.
 * {@code select=*,soaps(name)&status=eq.PENDING&order=created_at.desc&limit=20}.
 */
public final class Query {

    private final List<String> parts = new ArrayList<>();

    /**
     * @param columns PostgREST select list, e.g. "*,profiles(full_name)"
     * @return a new query
     */
    public static Query select(String columns) {
        Query q = new Query();
        q.parts.add("select=" + UrlEncoder.encode(columns));
        return q;
    }

    /** @return a query without a select list (for PATCH / DELETE filters) */
    public static Query filter() {
        return new Query();
    }

    /**
     * @param column column name
     * @param value  value
     * @return this query
     */
    public Query eq(String column, Object value) {
        return op(column, "eq", String.valueOf(value));
    }

    /**
     * @param column column name
     * @param value  value
     * @return this query
     */
    public Query neq(String column, Object value) {
        return op(column, "neq", String.valueOf(value));
    }

    /**
     * @param column column name
     * @param value  value
     * @return this query
     */
    public Query gte(String column, Object value) {
        return op(column, "gte", String.valueOf(value));
    }

    /**
     * @param column column name
     * @param value  value (exclusive upper bound)
     * @return this query
     */
    public Query lt(String column, Object value) {
        return op(column, "lt", String.valueOf(value));
    }

    /**
     * @param column column name
     * @param values allowed values
     * @return this query
     */
    public Query in(String column, Object... values) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        sb.append(')');
        return op(column, "in", sb.toString());
    }

    /**
     * @param column column name
     * @return this query (column IS NULL)
     */
    public Query isNull(String column) {
        return op(column, "is", "null");
    }

    /**
     * Case-insensitive "contains" on several columns, for search boxes.
     *
     * @param text    search text
     * @param columns columns to search
     * @return this query
     */
    public Query search(String text, String... columns) {
        String safe = text.replace(",", " ").replace("(", " ").replace(")", " ").replace("*", " ")
                .trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(columns[i]).append(".ilike.*").append(safe).append('*');
        }
        sb.append(')');
        parts.add("or=" + UrlEncoder.encode(sb.toString()));
        return this;
    }

    /**
     * @param column     column name
     * @param descending true for newest first
     * @return this query
     */
    public Query order(String column, boolean descending) {
        parts.add("order=" + UrlEncoder.encode(column + (descending ? ".desc" : ".asc")));
        return this;
    }

    /**
     * @param count maximum rows
     * @return this query
     */
    public Query limit(int count) {
        parts.add("limit=" + count);
        return this;
    }

    private Query op(String column, String operator, String value) {
        parts.add(UrlEncoder.encode(column) + "=" + operator + "." + UrlEncoder.encode(value));
        return this;
    }

    /** @return the query string without a leading "?" */
    public String build() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return build();
    }
}
