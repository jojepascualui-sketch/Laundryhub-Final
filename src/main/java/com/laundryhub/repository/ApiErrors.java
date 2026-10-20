package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.util.Json;
import com.laundryhub.util.JsonObject;

/**
 * Turns Supabase error responses into {@link ApiException}s with messages a customer understands.
 *
 * <p>Formats handled: PostgREST {@code {"code","message"}}, Auth {@code {"error_description"}} or
 * {@code {"msg"}}, Storage {@code {"error","message"}}.
 */
public final class ApiErrors {

    /** Message for network failures. */
    public static final String OFFLINE_MESSAGE = "No internet connection. Please check your connection and try again.";

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int SERVER_ERROR = 500;

    private ApiErrors() {
    }

    /** @return the error used when the network request failed */
    public static ApiException offline() {
        return new ApiException(ApiException.NETWORK_ERROR, "", OFFLINE_MESSAGE);
    }

    /**
     * @param response a non-2xx response
     * @return an exception with a friendly message
     */
    public static ApiException fromResponse(HttpResponse response) {
        int status = response.getStatus();
        String code = "";
        String message = "";
        try {
            Object parsed = Json.parse(response.getBody());
            if (parsed instanceof JsonObject) {
                JsonObject o = (JsonObject) parsed;
                code = firstText(o, "code", "error_code", "error", "statusCode");
                message = firstText(o, "message", "error_description", "msg", "error");
            }
        } catch (RuntimeException notJson) {
            message = response.getBody();
        }
        return new ApiException(status, code, friendly(status, code, message));
    }

    private static String friendly(int status, String code, String message) {
        String m = message == null ? "" : message;
        String lower = m.toLowerCase();
        if (lower.contains("invalid login credentials")) {
            return "Wrong email or password.";
        }
        if (lower.contains("email not confirmed")) {
            return "Please confirm your email first. Check your inbox for the link.";
        }
        if (lower.contains("already registered") || lower.contains("already been registered")) {
            return "An account with this email already exists. Try signing in.";
        }
        if (lower.contains("row-level security") || (status == FORBIDDEN && m.isEmpty())) {
            return "You don't have permission to do that.";
        }
        if (lower.contains("jwt expired") || (status == UNAUTHORIZED && m.isEmpty())) {
            return "Your session has ended. Please sign in again.";
        }
        if (lower.contains("rate limit")) {
            return "Too many attempts. Please wait a minute and try again.";
        }
        if (lower.contains("invalid api key") || lower.contains("no api key")) {
            return "The app is not connected to the database yet (check the Supabase key).";
        }
        if (m.isEmpty()) {
            return status >= SERVER_ERROR ? "The server had a problem. Please try again." : "Something went wrong. Please try again.";
        }
        return m;
    }

    private static String firstText(JsonObject o, String... keys) {
        for (String k : keys) {
            String v = o.getString(k);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return "";
    }
}
