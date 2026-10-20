package com.laundryhub.service;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.AuthSession;
import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import com.laundryhub.repository.ProfileRepository;
import com.laundryhub.repository.SupabaseClient;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.Validators;

/**
 * Sign in, sign up and sign out, plus the rule that the chosen tab (Customer / Rider / Admin)
 * must match the account's real role.
 */
public final class AuthService {

    /** Result of sign up: either signed in, or waiting for the e-mail confirmation link. */
    public static final class SignUpResult {
        private final User user;

        SignUpResult(User user) {
            this.user = user;
        }

        /** @return the new user, or null when the e-mail must be confirmed first */
        public User getUser() {
            return user;
        }

        /** @return true when the user must click the link in their e-mail */
        public boolean needsEmailConfirmation() {
            return user == null;
        }
    }

    private static final int BAD_REQUEST = 400;
    private static final int FORBIDDEN = 403;

    private final SupabaseClient client;
    private final ProfileRepository profiles;

    /**
     * @param client   Supabase client
     * @param profiles profile repository
     */
    public AuthService(SupabaseClient client, ProfileRepository profiles) {
        this.client = client;
        this.profiles = profiles;
    }

    /**
     * The tab a role signs in with.
     *
     * @param role account role
     * @return CUSTOMER, RIDER or ADMIN (STAFF uses the Admin tab)
     */
    public static Role tabFor(Role role) {
        return role == Role.STAFF ? Role.ADMIN : role;
    }

    /**
     * @param tab    chosen tab
     * @param actual the account's role
     * @return a friendly message when they do not match, otherwise null
     */
    public static String roleMismatchMessage(Role tab, Role actual) {
        Role expected = tabFor(actual);
        if (expected == tabFor(tab)) {
            return null;
        }
        String name;
        switch (expected) {
            case RIDER:
                name = "Rider";
                break;
            case ADMIN:
                name = "Admin";
                break;
            default:
                name = "Customer";
        }
        return "This is a " + name.toLowerCase() + " account. We switched to the " + name
                + " tab. Tap Sign in again.";
    }

    /**
     * Signs in, loads the profile and checks the role against the chosen tab.
     * On a mismatch the session is closed again and the error message explains which tab to use.
     *
     * @param tab        chosen tab
     * @param login      e-mail (or a rider's mobile on the Rider tab)
     * @param password   password
     * @param rememberMe keep the session after the browser closes
     * @param callback   receives the user
     */
    public void signIn(Role tab, String login, String password, boolean rememberMe, Callback<User> callback) {
        String value = login == null ? "" : login.trim();
        if (tab == Role.RIDER && Validators.isMobile(value)) {
            client.rpc("rider_login_email", new JsonObject().put("p_mobile", Validators.compact(value)),
                    new Callback<Object>() {
                        @Override
                        public void onSuccess(Object email) {
                            if (!(email instanceof String) || ((String) email).isEmpty()) {
                                callback.onError(new ApiException(BAD_REQUEST, "",
                                        "No rider account uses that mobile number."));
                            } else {
                                signInWithEmail(tab, (String) email, password, rememberMe, callback);
                            }
                        }

                        @Override
                        public void onError(ApiException error) {
                            callback.onError(error);
                        }
                    });
            return;
        }
        signInWithEmail(tab, value, password, rememberMe, callback);
    }

    private void signInWithEmail(Role tab, String email, String password, boolean rememberMe, Callback<User> callback) {
        client.signInWithPassword(email, password, rememberMe, new Callback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession session) {
                loadAndCheck(session.getUserId(), tab, callback);
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Loads the signed-in user's profile (after a redirect or on page load).
     *
     * @param callback receives the user
     */
    public void loadCurrentUser(Callback<User> callback) {
        if (client.getSession() == null || client.getSession().getUserId() == null) {
            callback.onError(new ApiException(401, "", "Please sign in."));
            return;
        }
        loadAndCheck(client.getSession().getUserId(), null, callback);
    }

    private void loadAndCheck(String userId, Role tab, Callback<User> callback) {
        profiles.findUser(userId, new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!user.isActive()) {
                    signOutQuietly();
                    callback.onError(new ApiException(FORBIDDEN, "",
                            "This account is turned off. Please contact the shop."));
                    return;
                }
                String mismatch = tab == null ? null : roleMismatchMessage(tab, user.getRole());
                if (mismatch != null) {
                    signOutQuietly();
                    callback.onError(new ApiException(FORBIDDEN, "ROLE_MISMATCH:" + tabFor(user.getRole()).name(), mismatch));
                    return;
                }
                callback.onSuccess(user);
            }

            @Override
            public void onError(ApiException error) {
                signOutQuietly();
                callback.onError(error);
            }
        });
    }

    /**
     * Creates a customer account. full_name and mobile are saved to profiles by the database trigger.
     *
     * @param fullName full name
     * @param mobile   mobile 09XXXXXXXXX
     * @param email    e-mail
     * @param password password (8+ characters)
     * @param callback receives the result
     */
    public void signUp(String fullName, String mobile, String email, String password, Callback<SignUpResult> callback) {
        JsonObject meta = new JsonObject()
                .put("full_name", fullName.trim())
                .put("mobile", Validators.compact(mobile));
        client.signUp(email, password, meta, new Callback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession session) {
                if (session == null) {
                    callback.onSuccess(new SignUpResult(null));
                    return;
                }
                loadAndCheck(session.getUserId(), null, new Callback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        callback.onSuccess(new SignUpResult(user));
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    /**
     * @param redirectTo page to return to after Google
     * @return URL that starts Google sign-in
     */
    public String googleSignInUrl(String redirectTo) {
        return client.getGoogleSignInUrl(redirectTo);
    }

    /**
     * @param email      e-mail
     * @param redirectTo page the reset link opens
     * @param callback   called when the e-mail was sent
     */
    public void sendPasswordReset(String email, String redirectTo, Callback<Void> callback) {
        client.sendPasswordReset(email, redirectTo, callback);
    }

    /**
     * @param newPassword new password
     * @param callback    called when saved
     */
    public void changePassword(String newPassword, Callback<Void> callback) {
        client.updatePassword(newPassword, callback);
    }

    /** @param callback called when signed out */
    public void signOut(Callback<Void> callback) {
        client.signOut(callback);
    }

    private void signOutQuietly() {
        client.signOut(new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(ApiException error) {
            }
        });
    }
}
