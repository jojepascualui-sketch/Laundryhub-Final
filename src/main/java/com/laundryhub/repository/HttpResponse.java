package com.laundryhub.repository;

/**
 * Status and text of an HTTP response.
 */
public final class HttpResponse {

    private static final int OK_MIN = 200;
    private static final int OK_MAX = 299;

    private final int status;
    private final String body;

    /**
     * @param status HTTP status code
     * @param body   response text, may be empty
     */
    public HttpResponse(int status, String body) {
        this.status = status;
        this.body = body == null ? "" : body;
    }

    /** @return HTTP status code */
    public int getStatus() {
        return status;
    }

    /** @return response text, never null */
    public String getBody() {
        return body;
    }

    /** @return true for 2xx */
    public boolean isSuccess() {
        return status >= OK_MIN && status <= OK_MAX;
    }
}
