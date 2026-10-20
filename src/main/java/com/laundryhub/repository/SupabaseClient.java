package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.LaundryHubException;
import com.laundryhub.model.AuthSession;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Json;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.UrlEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Talks to Supabase over HTTPS: Auth ({@code /auth/v1}), the database REST API
 * ({@code /rest/v1}, incl. rpc) and Storage ({@code /storage/v1}).
 *
 * <p>Only the publishable key is used. It is sent as the {@code apikey} header; the
 * {@code Authorization} header carries the signed-in user's access token, which is refreshed
 * automatically before it expires.
 *
 * <p>OOP: Encapsulation — tokens and the refresh logic are private; repositories just call
 * {@link #select}, {@link #rpc} and friends.
 */
public class SupabaseClient {

    /** Listens for sign in / sign out (e.g. to send the user back to the sign-in page). */
    public interface SessionListener {
        /**
         * @param session the new session, or null after sign out / expiry
         */
        void onSessionChanged(AuthSession session);
    }

    private static final String JSON = "application/json";
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long DEFAULT_EXPIRES_IN = 3600;

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;
    private final SessionStore store;
    private final LongSupplier clockSeconds;
    private final List<SessionListener> listeners = new ArrayList<>();

    private AuthSession session;
    private boolean remember = true;
    private boolean refreshing;
    private final List<Callback<AuthSession>> waitingForRefresh = new ArrayList<>();

    /**
     * @param baseUrl Supabase project URL, e.g. https://xxxx.supabase.co
     * @param apiKey  publishable key (sb_publishable_...)
     * @param http    HTTP client
     * @param store   where the session is kept between page loads
     */
    public SupabaseClient(String baseUrl, String apiKey, HttpClient http, SessionStore store) {
        this(baseUrl, apiKey, http, store, () -> System.currentTimeMillis() / MILLIS_PER_SECOND);
    }

    /**
     * Constructor with a clock, for tests.
     *
     * @param baseUrl      Supabase project URL
     * @param apiKey       publishable key
     * @param http         HTTP client
     * @param store        session store
     * @param clockSeconds current time in epoch seconds
     */
    public SupabaseClient(String baseUrl, String apiKey, HttpClient http, SessionStore store, LongSupplier clockSeconds) {
        if (baseUrl == null || baseUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            throw new LaundryHubException("Supabase URL and publishable key are not set (config.properties).");
        }
        if (apiKey.startsWith("sb_secret_")) {
            throw new LaundryHubException("Never use the Supabase secret key in the website. Use the publishable key.");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.http = http;
        this.store = store;
        this.clockSeconds = clockSeconds;
        restoreSession();
    }

    // ---- session ------------------------------------------------------------

    /** @return the current session, or null when signed out */
    public AuthSession getSession() {
        return session;
    }

    /** @return true when a user is signed in */
    public boolean isSignedIn() {
        return session != null;
    }

    /** @param listener called on every sign in / sign out */
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    private void restoreSession() {
        String saved = store.load();
        if (saved == null || saved.isEmpty()) {
            return;
        }
        try {
            JsonObject o = Json.parseObject(saved);
            session = new AuthSession(o.getString("access_token"), o.getString("refresh_token"),
                    o.getLong("expires_at", 0), o.getString("user_id"), o.getString("email"));
            remember = o.getBoolean("remember", true);
        } catch (RuntimeException corrupt) {
            store.clear();
        }
    }

    private void setSession(AuthSession newSession) {
        session = newSession;
        if (newSession == null) {
            store.clear();
        } else {
            JsonObject o = new JsonObject()
                    .put("access_token", newSession.getAccessToken())
                    .put("refresh_token", newSession.getRefreshToken())
                    .put("expires_at", newSession.getExpiresAt())
                    .put("user_id", newSession.getUserId())
                    .put("email", newSession.getEmail())
                    .put("remember", remember);
            store.save(o.toString(), remember);
        }
        for (SessionListener l : new ArrayList<>(listeners)) {
            l.onSessionChanged(newSession);
        }
    }

    private AuthSession sessionFromTokenResponse(JsonObject o) {
        long now = clockSeconds.getAsLong();
        long expiresAt = o.getLong("expires_at", now + o.getLong("expires_in", DEFAULT_EXPIRES_IN));
        JsonObject user = o.getObject("user");
        String userId = user == null ? null : user.getString("id");
        String email = user == null ? null : user.getString("email");
        return new AuthSession(o.getString("access_token"), o.getString("refresh_token"), expiresAt, userId, email);
    }

    // ---- auth ---------------------------------------------------------------

    /**
     * Creates an account. When e-mail confirmation is on, the result is null (no session yet).
     *
     * @param email    e-mail
     * @param password password
     * @param metadata user metadata saved by the database trigger (full_name, mobile)
     * @param callback receives the session, or null when the user must confirm the e-mail
     */
    public void signUp(String email, String password, JsonObject metadata, Callback<AuthSession> callback) {
        JsonObject body = new JsonObject().put("email", email.trim()).put("password", password)
                .put("data", metadata == null ? new JsonObject() : metadata);
        remember = true;
        sendPublic("POST", "/auth/v1/signup", body.toString(), callback, text -> {
            JsonObject o = Json.parseObject(text);
            if (o.isNull("access_token")) {
                return null;
            }
            AuthSession s = sessionFromTokenResponse(o);
            setSession(s);
            return s;
        });
    }

    /**
     * Signs in with e-mail and password.
     *
     * @param email      e-mail
     * @param password   password
     * @param rememberMe keep the session after the browser closes
     * @param callback   receives the session
     */
    public void signInWithPassword(String email, String password, boolean rememberMe, Callback<AuthSession> callback) {
        JsonObject body = new JsonObject().put("email", email.trim()).put("password", password);
        remember = rememberMe;
        sendPublic("POST", "/auth/v1/token?grant_type=password", body.toString(), callback, text -> {
            AuthSession s = sessionFromTokenResponse(Json.parseObject(text));
            setSession(s);
            return s;
        });
    }

    /**
     * @param redirectTo page Google should return to (must be in Supabase's redirect URL list)
     * @return URL to open for "Continue with Google"
     */
    public String getGoogleSignInUrl(String redirectTo) {
        return baseUrl + "/auth/v1/authorize?provider=google&redirect_to=" + UrlEncoder.encode(redirectTo);
    }

    /**
     * @param fragment the URL hash without "#"
     * @return true when it carries tokens from Google sign-in, an e-mail link or a password reset
     */
    public static boolean isAuthRedirect(String fragment) {
        return fragment != null && fragment.contains("access_token=");
    }

    /**
     * @param fragment the URL hash without "#"
     * @return true when the redirect came from a "reset password" e-mail
     */
    public static boolean isRecoveryRedirect(String fragment) {
        return isAuthRedirect(fragment) && fragment.contains("type=recovery");
    }

    /**
     * @param fragment the URL hash without "#"
     * @return the error text when Supabase redirected back with an error, else null
     */
    public static String getRedirectError(String fragment) {
        if (fragment == null || !fragment.contains("error")) {
            return null;
        }
        JsonObject p = parseFragment(fragment);
        String d = p.getString("error_description");
        return d != null ? d : p.getString("error");
    }

    /**
     * Finishes a redirect sign-in: stores the tokens from the URL, then loads the user.
     *
     * @param fragment the URL hash without "#"
     * @param callback receives the session
     */
    public void completeRedirectSignIn(String fragment, Callback<AuthSession> callback) {
        JsonObject p = parseFragment(fragment);
        String access = p.getString("access_token");
        String refresh = p.getString("refresh_token");
        if (access == null || refresh == null) {
            callback.onError(new ApiException(400, "", "The sign-in link is incomplete. Please try again."));
            return;
        }
        long now = clockSeconds.getAsLong();
        long expiresAt = p.has("expires_at") ? p.getLong("expires_at", 0) : now + p.getLong("expires_in", DEFAULT_EXPIRES_IN);
        remember = true;
        AuthSession partial = new AuthSession(access, refresh, expiresAt, null, null);
        session = partial;
        send("GET", "/auth/v1/user", null, new Callback<String>() {
            @Override
            public void onSuccess(String text) {
                JsonObject user = Json.parseObject(text);
                AuthSession full = new AuthSession(access, refresh, expiresAt, user.getString("id"), user.getString("email"));
                setSession(full);
                callback.onSuccess(full);
            }

            @Override
            public void onError(ApiException error) {
                session = null;
                callback.onError(error);
            }
        });
    }

    /**
     * Sends a "reset your password" e-mail.
     *
     * @param email      e-mail
     * @param redirectTo page the e-mail link opens
     * @param callback   called when sent
     */
    public void sendPasswordReset(String email, String redirectTo, Callback<Void> callback) {
        JsonObject body = new JsonObject().put("email", email.trim());
        sendPublic("POST", "/auth/v1/recover?redirect_to=" + UrlEncoder.encode(redirectTo), body.toString(),
                callback, text -> null);
    }

    /**
     * Changes the signed-in user's password (after a reset link).
     *
     * @param newPassword new password
     * @param callback    called when saved
     */
    public void updatePassword(String newPassword, Callback<Void> callback) {
        send("PUT", "/auth/v1/user", new JsonObject().put("password", newPassword).toString(),
                map(callback, text -> null));
    }

    /**
     * Signs out on the server (best effort) and forgets the session locally.
     *
     * @param callback called when done (never fails)
     */
    public void signOut(Callback<Void> callback) {
        if (session == null) {
            callback.onSuccess(null);
            return;
        }
        HttpRequest req = new HttpRequest("POST", baseUrl + "/auth/v1/logout")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + session.getAccessToken());
        setSession(null);
        http.send(req, new Callback<HttpResponse>() {
            @Override
            public void onSuccess(HttpResponse result) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(ApiException error) {
                callback.onSuccess(null);
            }
        });
    }

    /**
     * Gets a fresh access token from the refresh token. Concurrent callers share one request.
     *
     * @param callback receives the new session
     */
    public void refreshSession(Callback<AuthSession> callback) {
        if (session == null) {
            callback.onError(new ApiException(401, "", "Please sign in."));
            return;
        }
        waitingForRefresh.add(callback);
        if (refreshing) {
            return;
        }
        refreshing = true;
        JsonObject body = new JsonObject().put("refresh_token", session.getRefreshToken());
        HttpRequest req = new HttpRequest("POST", baseUrl + "/auth/v1/token?grant_type=refresh_token")
                .header("apikey", apiKey)
                .header("Content-Type", JSON)
                .body(body.toString());
        http.send(req, new Callback<HttpResponse>() {
            @Override
            public void onSuccess(HttpResponse response) {
                if (response.isSuccess()) {
                    AuthSession s = sessionFromTokenResponse(Json.parseObject(response.getBody()));
                    finishRefresh(s, null);
                } else {
                    ApiException err = ApiErrors.fromResponse(response);
                    if (response.getStatus() >= 400 && response.getStatus() < 500) {
                        setSession(null); // refresh token revoked or expired
                        err = new ApiException(401, err.getCode(), "Your session has ended. Please sign in again.");
                    }
                    finishRefresh(null, err);
                }
            }

            @Override
            public void onError(ApiException error) {
                finishRefresh(null, error);
            }
        });
    }

    private void finishRefresh(AuthSession s, ApiException error) {
        refreshing = false;
        if (s != null) {
            setSession(s);
        }
        List<Callback<AuthSession>> waiting = new ArrayList<>(waitingForRefresh);
        waitingForRefresh.clear();
        for (Callback<AuthSession> cb : waiting) {
            if (error == null) {
                cb.onSuccess(s);
            } else {
                cb.onError(error);
            }
        }
    }

    // ---- database (PostgREST) ------------------------------------------------

    /**
     * {@code GET /rest/v1/<table>?<query>}
     *
     * @param table    table name
     * @param query    select, filters, order
     * @param callback receives the rows (RLS decides which ones)
     */
    public void select(String table, Query query, Callback<JsonArray> callback) {
        send("GET", "/rest/v1/" + table + "?" + query.build(), null, map(callback, Json::parseArray));
    }

    /**
     * Like {@link #select} but returns the first row, or null.
     *
     * @param table    table name
     * @param query    select and filters
     * @param callback receives the row or null
     */
    public void selectOne(String table, Query query, Callback<JsonObject> callback) {
        select(table, query.limit(1), mapRows(callback, rows -> rows.isEmpty() ? null : rows.getObject(0)));
    }

    /**
     * {@code POST /rest/v1/<table>} and returns the inserted rows.
     *
     * @param table    table name
     * @param row      row values
     * @param callback receives the inserted rows
     */
    public void insert(String table, JsonObject row, Callback<JsonArray> callback) {
        send("POST", "/rest/v1/" + table, row.toString(), map(callback, Json::parseArray));
    }

    /**
     * {@code PATCH /rest/v1/<table>?<filter>} and returns the changed rows.
     *
     * @param table    table name
     * @param filter   which rows (e.g. {@code Query.filter().eq("id", 5)})
     * @param changes  new values
     * @param callback receives the changed rows (empty when RLS blocked it)
     */
    public void update(String table, Query filter, JsonObject changes, Callback<JsonArray> callback) {
        send("PATCH", "/rest/v1/" + table + "?" + filter.build(), changes.toString(), map(callback, Json::parseArray));
    }

    /**
     * {@code DELETE /rest/v1/<table>?<filter>}
     *
     * @param table    table name
     * @param filter   which rows
     * @param callback called when done
     */
    public void delete(String table, Query filter, Callback<Void> callback) {
        send("DELETE", "/rest/v1/" + table + "?" + filter.build(), null, map(callback, text -> null));
    }

    /**
     * Calls a database function: {@code POST /rest/v1/rpc/<name>}.
     *
     * @param function function name, e.g. place_order
     * @param params   named parameters (p_...)
     * @param callback receives the parsed JSON result (object, array, value or null)
     */
    public void rpc(String function, JsonObject params, Callback<Object> callback) {
        String body = (params == null ? new JsonObject() : params).toString();
        send("POST", "/rest/v1/rpc/" + function, body,
                map(callback, text -> text.trim().isEmpty() ? null : Json.parse(text)));
    }

    // ---- storage ------------------------------------------------------------

    /**
     * Uploads a browser File/Blob, replacing any file at the same path.
     *
     * @param bucket      bucket name
     * @param path        object path inside the bucket, e.g. "&lt;user id&gt;/LH-0001.jpg"
     * @param browserFile File or Blob from an input element
     * @param contentType MIME type, e.g. image/jpeg
     * @param callback    receives the path
     */
    public void upload(String bucket, String path, Object browserFile, String contentType, Callback<String> callback) {
        withFreshSession(callback, () -> {
            HttpRequest req = authorized(new HttpRequest("POST", baseUrl + "/storage/v1/object/" + bucket + "/" + path))
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .binaryBody(browserFile);
            http.send(req, handle(map(callback, text -> path), null));
        });
    }

    /**
     * Creates a temporary link to a private file (e.g. a GCash screenshot for the admin).
     *
     * @param bucket    bucket name
     * @param path      object path
     * @param seconds   how long the link works
     * @param callback  receives the full URL
     */
    public void createSignedUrl(String bucket, String path, int seconds, Callback<String> callback) {
        String body = new JsonObject().put("expiresIn", seconds).toString();
        send("POST", "/storage/v1/object/sign/" + bucket + "/" + path, body, map(callback, text -> {
            String signed = Json.parseObject(text).getString("signedURL");
            return signed == null ? null : baseUrl + "/storage/v1" + signed;
        }));
    }

    // ---- plumbing -------------------------------------------------------------

    /** Converts response text into a result. */
    private interface Parser<T> {
        T parse(String text);
    }

    private HttpRequest authorized(HttpRequest req) {
        req.header("apikey", apiKey);
        if (session != null) {
            req.header("Authorization", "Bearer " + session.getAccessToken());
        }
        return req;
    }

    /** Sends a signed-in request (refreshing the token first if needed). */
    private void send(String method, String path, String body, Callback<String> callback) {
        withFreshSession(callback, () -> sendNow(method, path, body, callback, true));
    }

    private void sendNow(String method, String path, String body, Callback<String> callback, boolean mayRetry) {
        HttpRequest req = authorized(new HttpRequest(method, baseUrl + path));
        if (body != null) {
            req.header("Content-Type", JSON).body(body);
        }
        if (path.startsWith("/rest/v1/") && !"GET".equals(method)) {
            req.header("Prefer", "return=representation");
        }
        Runnable retry = mayRetry && session != null
                ? () -> refreshSession(new Callback<AuthSession>() {
                    @Override
                    public void onSuccess(AuthSession s) {
                        sendNow(method, path, body, callback, false);
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                })
                : null;
        http.send(req, handle(callback, retry));
    }

    /** Sends a request that does not need a signed-in user (sign in, sign up, reset). */
    private <T> void sendPublic(String method, String path, String body, Callback<T> callback, Parser<T> parser) {
        HttpRequest req = new HttpRequest(method, baseUrl + path)
                .header("apikey", apiKey)
                .header("Content-Type", JSON)
                .body(body);
        http.send(req, handle(map(callback, parser), null));
    }

    private <T> void withFreshSession(Callback<T> callback, Runnable action) {
        if (session != null && session.needsRefresh(clockSeconds.getAsLong())) {
            refreshSession(new Callback<AuthSession>() {
                @Override
                public void onSuccess(AuthSession s) {
                    action.run();
                }

                @Override
                public void onError(ApiException error) {
                    callback.onError(error);
                }
            });
        } else {
            action.run();
        }
    }

    /** Turns an HTTP response into text or an ApiException; retries once on 401 when possible. */
    private Callback<HttpResponse> handle(Callback<String> callback, Runnable retryAfterRefresh) {
        return new Callback<HttpResponse>() {
            @Override
            public void onSuccess(HttpResponse response) {
                if (response.isSuccess()) {
                    callback.onSuccess(response.getBody());
                } else if (response.getStatus() == 401 && retryAfterRefresh != null) {
                    retryAfterRefresh.run();
                } else {
                    callback.onError(ApiErrors.fromResponse(response));
                }
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        };
    }

    private static <T> Callback<String> map(Callback<T> callback, Parser<T> parser) {
        return new Callback<String>() {
            @Override
            public void onSuccess(String text) {
                T value;
                try {
                    value = parser.parse(text);
                } catch (RuntimeException e) {
                    callback.onError(new ApiException(500, "", "Unexpected answer from the server."));
                    return;
                }
                callback.onSuccess(value);
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        };
    }

    private static <T> Callback<JsonArray> mapRows(Callback<T> callback, Function<JsonArray, T> mapper) {
        return new Callback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray rows) {
                callback.onSuccess(mapper.apply(rows));
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        };
    }

    private static JsonObject parseFragment(String fragment) {
        JsonObject o = new JsonObject();
        String f = fragment.startsWith("#") ? fragment.substring(1) : fragment;
        int q = f.indexOf("access_token=");
        int err = f.indexOf("error");
        int start = q >= 0 ? q : (err >= 0 ? err : 0);
        // The router may prefix the hash with a route (e.g. "/signin#access_token=..." or "/?error=...").
        int lastSep = Math.max(f.lastIndexOf('#', start), f.lastIndexOf('?', start));
        String params = lastSep >= 0 && lastSep < start ? f.substring(lastSep + 1) : f.substring(start);
        for (String pair : params.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                o.put(UrlEncoder.decode(pair.substring(0, eq)), UrlEncoder.decode(pair.substring(eq + 1)));
            }
        }
        return o;
    }
}
