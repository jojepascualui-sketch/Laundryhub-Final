package com.laundryhub.repository;

import com.laundryhub.util.Callback;

/**
 * Sends HTTP requests.
 *
 * <p>OOP: Abstraction — {@link SupabaseClient} only knows this interface. In the browser it is
 * {@link XhrHttpClient}; in JUnit tests it is a fake that returns prepared answers.
 */
public interface HttpClient {

    /**
     * Sends the request. Any HTTP status (including 4xx/5xx) is a success here;
     * {@link Callback#onError} is only for network failures.
     *
     * @param request  the request
     * @param callback receives the response
     */
    void send(HttpRequest request, Callback<HttpResponse> callback);
}
