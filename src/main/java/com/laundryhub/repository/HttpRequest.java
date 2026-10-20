package com.laundryhub.repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An HTTP request described in plain Java, sent by an {@link HttpClient}.
 */
public final class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String textBody;
    private Object binaryBody;

    /**
     * @param method GET, POST, PATCH or DELETE
     * @param url    full URL
     */
    public HttpRequest(String method, String url) {
        this.method = method;
        this.url = url;
    }

    /**
     * @param name  header name
     * @param value header value
     * @return this request
     */
    public HttpRequest header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * @param body JSON or text body
     * @return this request
     */
    public HttpRequest body(String body) {
        this.textBody = body;
        return this;
    }

    /**
     * @param browserFile a browser File/Blob object (only the browser HttpClient understands it)
     * @return this request
     */
    public HttpRequest binaryBody(Object browserFile) {
        this.binaryBody = browserFile;
        return this;
    }

    /** @return HTTP method */
    public String getMethod() {
        return method;
    }

    /** @return URL */
    public String getUrl() {
        return url;
    }

    /** @return headers in insertion order */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /** @return text body, may be null */
    public String getTextBody() {
        return textBody;
    }

    /** @return binary body, may be null */
    public Object getBinaryBody() {
        return binaryBody;
    }
}
