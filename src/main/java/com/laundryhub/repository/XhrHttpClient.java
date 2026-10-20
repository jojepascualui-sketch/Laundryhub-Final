package com.laundryhub.repository;

import com.laundryhub.util.Callback;
import java.util.Map;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;

/**
 * {@link HttpClient} for the browser, using XMLHttpRequest through TeaVM JSO.
 *
 * <p>OOP: Polymorphism — SupabaseClient calls {@link #send} without knowing this is the browser.
 */
public final class XhrHttpClient implements HttpClient {

    private static final int TIMEOUT_MILLIS = 30000;

    @Override
    public void send(HttpRequest request, Callback<HttpResponse> callback) {
        XMLHttpRequest xhr = new XMLHttpRequest();
        xhr.open(request.getMethod(), request.getUrl());
        for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
            xhr.setRequestHeader(h.getKey(), h.getValue());
        }
        setTimeout(xhr, TIMEOUT_MILLIS);
        xhr.addEventListener("load", e -> callback.onSuccess(new HttpResponse(xhr.getStatus(), xhr.getResponseText())));
        xhr.addEventListener("error", e -> callback.onError(ApiErrors.offline()));
        xhr.addEventListener("timeout", e -> callback.onError(ApiErrors.offline()));

        if (request.getBinaryBody() != null) {
            xhr.send((JSObject) request.getBinaryBody());
        } else if (request.getTextBody() != null) {
            xhr.send(request.getTextBody());
        } else {
            xhr.send();
        }
    }

    @JSBody(params = {"xhr", "ms"}, script = "xhr.timeout = ms;")
    private static native void setTimeout(XMLHttpRequest xhr, int ms);
}
