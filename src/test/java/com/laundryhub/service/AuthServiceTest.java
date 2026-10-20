package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Admin;
import com.laundryhub.model.Customer;
import com.laundryhub.model.Rider;
import com.laundryhub.model.Role;
import com.laundryhub.model.Staff;
import com.laundryhub.model.User;
import com.laundryhub.repository.HttpClient;
import com.laundryhub.repository.HttpRequest;
import com.laundryhub.repository.HttpResponse;
import com.laundryhub.repository.ProfileRepository;
import com.laundryhub.repository.SessionStore;
import com.laundryhub.repository.SupabaseClient;
import com.laundryhub.util.Callback;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final String TOKEN = "{\"access_token\":\"A\",\"refresh_token\":\"R\",\"expires_at\":99999,"
            + "\"user\":{\"id\":\"u1\",\"email\":\"x@y.co\"}}";

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
            callback.onSuccess(r == null ? new HttpResponse(204, "") : r);
        }
    }

    static final class MemoryStore implements SessionStore {
        String value;

        @Override
        public String load() {
            return value;
        }

        @Override
        public void save(String sessionJson, boolean remember) {
            value = sessionJson;
        }

        @Override
        public void clear() {
            value = null;
        }
    }

    static final class FakeProfiles implements ProfileRepository {
        User user;

        @Override
        public void findUser(String userId, Callback<User> callback) {
            callback.onSuccess(user);
        }

        @Override
        public void updateOwnProfile(String userId, String fullName, String mobile, Callback<Void> callback) {
            callback.onSuccess(null);
        }
    }

    static final class Result<T> implements Callback<T> {
        T value;
        ApiException error;

        @Override
        public void onSuccess(T result) {
            value = result;
        }

        @Override
        public void onError(ApiException e) {
            error = e;
        }
    }

    private FakeHttp http;
    private SupabaseClient client;
    private FakeProfiles profiles;
    private AuthService auth;

    private void setUp(User user) {
        http = new FakeHttp();
        client = new SupabaseClient("https://demo.supabase.co", "sb_publishable_x", http, new MemoryStore(), () -> 1000);
        profiles = new FakeProfiles();
        profiles.user = user;
        auth = new AuthService(client, profiles);
    }

    @Test
    void customerOnCustomerTabSignsIn() {
        setUp(new Customer("u1", "Ana Reyes", "x@y.co", null, true));
        http.answer(200, TOKEN);
        Result<User> r = new Result<>();
        auth.signIn(Role.CUSTOMER, "x@y.co", "password1", true, r);
        assertInstanceOf(Customer.class, r.value);
        assertTrue(client.isSignedIn());
    }

    @Test
    void riderOnCustomerTabIsSignedOutWithHint() {
        setUp(new Rider("u1", "Rider One", "x@y.co", null, true, null, false));
        http.answer(200, TOKEN);
        Result<User> r = new Result<>();
        auth.signIn(Role.CUSTOMER, "x@y.co", "password1", true, r);
        assertNull(r.value);
        assertEquals("ROLE_MISMATCH:RIDER", r.error.getCode());
        assertTrue(r.error.getMessage().contains("Rider tab"));
        assertFalse(client.isSignedIn());
    }

    @Test
    void staffUsesAdminTab() {
        setUp(new Staff("u1", "Sam", "x@y.co", null, true, "Counter staff"));
        http.answer(200, TOKEN);
        Result<User> r = new Result<>();
        auth.signIn(Role.ADMIN, "x@y.co", "password1", false, r);
        assertInstanceOf(Admin.class, r.value);
        assertNull(AuthService.roleMismatchMessage(Role.ADMIN, Role.STAFF));
        assertEquals(Role.ADMIN, AuthService.tabFor(Role.STAFF));
    }

    @Test
    void riderCanUseMobileNumber() {
        setUp(new Rider("u1", "Rider One", "rider@y.co", "09171234567", true, null, true));
        http.answer(200, "\"rider@y.co\"").answer(200, TOKEN);
        Result<User> r = new Result<>();
        auth.signIn(Role.RIDER, "0917 123 4567", "password1", true, r);
        assertInstanceOf(Rider.class, r.value);
        assertTrue(http.requests.get(0).getUrl().endsWith("/rest/v1/rpc/rider_login_email"));
        assertEquals("{\"p_mobile\":\"09171234567\"}", http.requests.get(0).getTextBody());
        assertTrue(http.requests.get(1).getTextBody().contains("rider@y.co"));
    }

    @Test
    void unknownRiderMobile() {
        setUp(null);
        http.answer(200, "null");
        Result<User> r = new Result<>();
        auth.signIn(Role.RIDER, "09170000000", "password1", true, r);
        assertEquals("No rider account uses that mobile number.", r.error.getMessage());
    }

    @Test
    void deactivatedAccountIsRefused() {
        setUp(new Customer("u1", "Old", "x@y.co", null, false));
        http.answer(200, TOKEN);
        Result<User> r = new Result<>();
        auth.signIn(Role.CUSTOMER, "x@y.co", "password1", true, r);
        assertTrue(r.error.getMessage().contains("turned off"));
        assertFalse(client.isSignedIn());
    }

    @Test
    void signUpWaitingForEmailConfirmation() {
        setUp(null);
        http.answer(200, "{\"id\":\"new\",\"email\":\"n@y.co\"}");
        Result<AuthService.SignUpResult> r = new Result<>();
        auth.signUp(" Juan Dela Cruz ", "0917 123 4567", "n@y.co", "password1", r);
        assertTrue(r.value.needsEmailConfirmation());
        assertTrue(http.requests.get(0).getTextBody()
                .contains("\"data\":{\"full_name\":\"Juan Dela Cruz\",\"mobile\":\"09171234567\"}"));
    }

    @Test
    void signUpWithImmediateSession() {
        setUp(new Customer("u1", "Juan", "n@y.co", "09171234567", true));
        http.answer(200, TOKEN);
        Result<AuthService.SignUpResult> r = new Result<>();
        auth.signUp("Juan", "09171234567", "n@y.co", "password1", r);
        assertFalse(r.value.needsEmailConfirmation());
        assertEquals("u1", r.value.getUser().getId());
    }

    @Test
    void mismatchMessages() {
        assertNull(AuthService.roleMismatchMessage(Role.CUSTOMER, Role.CUSTOMER));
        assertTrue(AuthService.roleMismatchMessage(Role.RIDER, Role.ADMIN).contains("Admin tab"));
        assertTrue(AuthService.roleMismatchMessage(Role.ADMIN, Role.CUSTOMER).contains("Customer tab"));
    }
}
