package com.laundryhub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.LaundryHubException;
import com.laundryhub.model.AuthSession;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupabaseClientTest {

    private static final String URL = "https://demo.supabase.co";
    private static final String KEY = "sb_publishable_test";

    /** Fake HTTP: records requests and answers from a queue. */
    static final class FakeHttp implements HttpClient {
        final List<HttpRequest> requests = new ArrayList<>();
        final Deque<HttpResponse> answers = new ArrayDeque<>();

        FakeHttp answer(int status, String body) {
            answers.add(new HttpResponse(status, body));
            return this;
        }

        @Override
        public void send(HttpRequest request, Callback<HttpResponse> callback) {
            requests.add(request);
            HttpResponse r = answers.poll();
            if (r == null) {
                callback.onError(ApiErrors.offline());
            } else {
                callback.onSuccess(r);
            }
        }
    }

    static final class MemoryStore implements SessionStore {
        String value;
        boolean remembered;

        @Override
        public String load() {
            return value;
        }

        @Override
        public void save(String sessionJson, boolean remember) {
            value = sessionJson;
            remembered = remember;
        }

        @Override
        public void clear() {
            value = null;
        }
    }

    /** Captures a callback result. */
    static final class Result<T> implements Callback<T> {
        T value;
        ApiException error;
        boolean done;

        @Override
        public void onSuccess(T result) {
            value = result;
            done = true;
        }

        @Override
        public void onError(ApiException e) {
            error = e;
            done = true;
        }
    }

    private static final String TOKEN_JSON =
            "{\"access_token\":\"AT1\",\"refresh_token\":\"RT1\",\"expires_in\":3600,\"expires_at\":5000,"
            + "\"user\":{\"id\":\"user-1\",\"email\":\"ana@example.com\"}}";

    @Test
    void signInStoresSessionAndSendsBearer() {
        FakeHttp http = new FakeHttp().answer(200, TOKEN_JSON).answer(200, "[{\"id\":1}]");
        MemoryStore store = new MemoryStore();
        SupabaseClient client = new SupabaseClient(URL, KEY, http, store, () -> 1000);

        Result<AuthSession> login = new Result<>();
        client.signInWithPassword(" ana@example.com ", "secret123", false, login);
        assertEquals("user-1", login.value.getUserId());
        assertTrue(client.isSignedIn());
        assertNotNull(store.value);
        assertFalse(store.remembered);

        HttpRequest first = http.requests.get(0);
        assertEquals(URL + "/auth/v1/token?grant_type=password", first.getUrl());
        assertEquals(KEY, first.getHeaders().get("apikey"));
        assertNull(first.getHeaders().get("Authorization"));
        assertTrue(first.getTextBody().contains("\"email\":\"ana@example.com\""));

        Result<JsonArray> rows = new Result<>();
        client.select("orders", Query.select("*").eq("status", "PENDING"), rows);
        assertEquals(1, rows.value.size());
        HttpRequest second = http.requests.get(1);
        assertEquals("Bearer AT1", second.getHeaders().get("Authorization"));
        assertEquals(URL + "/rest/v1/orders?select=*&status=eq.PENDING", second.getUrl());
    }

    @Test
    void wrongPasswordGivesFriendlyMessage() {
        FakeHttp http = new FakeHttp().answer(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, new MemoryStore(), () -> 1000);
        Result<AuthSession> r = new Result<>();
        client.signInWithPassword("a@b.co", "x", true, r);
        assertEquals("Wrong email or password.", r.error.getMessage());
        assertFalse(client.isSignedIn());
    }

    @Test
    void refreshesExpiredTokenBeforeRequest() {
        MemoryStore store = new MemoryStore();
        store.value = "{\"access_token\":\"OLD\",\"refresh_token\":\"RT0\",\"expires_at\":1010,\"user_id\":\"u\",\"remember\":true}";
        FakeHttp http = new FakeHttp().answer(200, TOKEN_JSON).answer(200, "{\"total\":410}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, store, () -> 1000);

        Result<Object> r = new Result<>();
        client.rpc("quote_order", new JsonObject().put("p_kg", 10), r);
        assertEquals(URL + "/auth/v1/token?grant_type=refresh_token", http.requests.get(0).getUrl());
        assertEquals("Bearer AT1", http.requests.get(1).getHeaders().get("Authorization"));
        assertEquals(410.0, ((JsonObject) r.value).getDouble("total", 0));
    }

    @Test
    void retriesOnceAfter401() {
        MemoryStore store = new MemoryStore();
        store.value = "{\"access_token\":\"OLD\",\"refresh_token\":\"RT0\",\"expires_at\":99999,\"user_id\":\"u\"}";
        FakeHttp http = new FakeHttp()
                .answer(401, "{\"message\":\"JWT expired\"}")
                .answer(200, TOKEN_JSON)
                .answer(200, "[]");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, store, () -> 1000);
        Result<JsonArray> r = new Result<>();
        client.select("soaps", Query.select("*"), r);
        assertNull(r.error);
        assertEquals(3, http.requests.size());
        assertEquals("Bearer AT1", http.requests.get(2).getHeaders().get("Authorization"));
    }

    @Test
    void revokedRefreshTokenSignsOut() {
        MemoryStore store = new MemoryStore();
        store.value = "{\"access_token\":\"OLD\",\"refresh_token\":\"RT0\",\"expires_at\":1,\"user_id\":\"u\"}";
        FakeHttp http = new FakeHttp().answer(400, "{\"error_description\":\"Invalid Refresh Token\"}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, store, () -> 1000);
        List<AuthSession> changes = new ArrayList<>();
        client.addSessionListener(changes::add);

        Result<JsonArray> r = new Result<>();
        client.select("orders", Query.select("*"), r);
        assertEquals(401, r.error.getStatus());
        assertFalse(client.isSignedIn());
        assertNull(store.value);
        assertEquals(1, changes.size());
        assertNull(changes.get(0));
    }

    @Test
    void rpcConflictKeepsServerMessage() {
        MemoryStore store = new MemoryStore();
        store.value = "{\"access_token\":\"A\",\"refresh_token\":\"R\",\"expires_at\":99999,\"user_id\":\"u\"}";
        FakeHttp http = new FakeHttp().answer(409, "{\"code\":\"PT409\",\"message\":\"This booking was already taken.\"}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, store, () -> 1000);
        Result<Object> r = new Result<>();
        client.rpc("accept_job", new JsonObject().put("p_job_id", 7), r);
        assertTrue(r.error.isConflict());
        assertEquals("This booking was already taken.", r.error.getMessage());
        assertEquals("return=representation", http.requests.get(0).getHeaders().get("Prefer"));
        assertEquals("{\"p_job_id\":7}", http.requests.get(0).getTextBody());
    }

    @Test
    void networkFailureIsOffline() {
        SupabaseClient client = new SupabaseClient(URL, KEY, new FakeHttp(), new MemoryStore(), () -> 1000);
        Result<JsonArray> r = new Result<>();
        client.select("soaps", Query.select("*"), r);
        assertEquals(ApiException.NETWORK_ERROR, r.error.getStatus());
    }

    @Test
    void redirectFragmentSignsIn() {
        FakeHttp http = new FakeHttp().answer(200, "{\"id\":\"g-user\",\"email\":\"g@gmail.com\"}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, new MemoryStore(), () -> 1000);
        String fragment = "access_token=GA&expires_at=4600&expires_in=3600&refresh_token=GR&token_type=bearer";
        assertTrue(SupabaseClient.isAuthRedirect(fragment));
        assertFalse(SupabaseClient.isRecoveryRedirect(fragment));
        Result<AuthSession> r = new Result<>();
        client.completeRedirectSignIn(fragment, r);
        assertEquals("g-user", r.value.getUserId());
        assertEquals(4600, r.value.getExpiresAt());
        assertEquals("Bearer GA", http.requests.get(0).getHeaders().get("Authorization"));
        assertTrue(SupabaseClient.isRecoveryRedirect("/x#access_token=a&refresh_token=b&type=recovery"));
        assertEquals("Email link is invalid or has expired",
                SupabaseClient.getRedirectError("error=access_denied&error_description=Email+link+is+invalid+or+has+expired"));
    }

    @Test
    void googleUrlAndSecretKeyGuard() {
        SupabaseClient client = new SupabaseClient(URL, KEY, new FakeHttp(), new MemoryStore(), () -> 1000);
        assertEquals(URL + "/auth/v1/authorize?provider=google&redirect_to=https%3A%2F%2Fapp.vercel.app%2F",
                client.getGoogleSignInUrl("https://app.vercel.app/"));
        assertThrows(LaundryHubException.class,
                () -> new SupabaseClient(URL, "sb_secret_x", new FakeHttp(), new MemoryStore(), () -> 0));
        assertThrows(LaundryHubException.class, () -> new SupabaseClient("", KEY, new FakeHttp(), new MemoryStore(), () -> 0));
    }

    @Test
    void signUpWithEmailConfirmationReturnsNoSession() {
        FakeHttp http = new FakeHttp().answer(200, "{\"id\":\"new\",\"email\":\"n@x.co\"}");
        SupabaseClient client = new SupabaseClient(URL, KEY, http, new MemoryStore(), () -> 1000);
        Result<AuthSession> r = new Result<>();
        client.signUp("n@x.co", "password1", new JsonObject().put("full_name", "N").put("mobile", "09171234567"), r);
        assertTrue(r.done);
        assertNull(r.value);
        assertNull(r.error);
        assertTrue(http.requests.get(0).getTextBody().contains("\"data\":{\"full_name\":\"N\",\"mobile\":\"09171234567\"}"));
    }

    @Test
    void queryBuilderEncodes() {
        assertEquals("select=*%2Csoaps(name)&status=in.(PENDING%2CREADY)&order=created_at.desc&limit=5",
                Query.select("*,soaps(name)").in("status", "PENDING", "READY").order("created_at", true).limit(5).build());
        assertEquals("customer_id=eq.abc&lat=is.null", Query.filter().eq("customer_id", "abc").isNull("lat").build());
        assertTrue(Query.select("*").search("Ana, (x)", "order_no", "address").build()
                .startsWith("select=*&or=(order_no.ilike.*Ana%20x*%2Caddress.ilike."));
    }
}
