package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.util.Callback;
import java.util.function.Consumer;

/**
 * Runs an action with the signed-in user's id, or reports "please sign in".
 */
final class AuthUser {

    private static final int UNAUTHORIZED = 401;

    private AuthUser() {
    }

    static <T> void require(SupabaseClient client, Callback<T> callback, Consumer<String> action) {
        if (client.getSession() == null || client.getSession().getUserId() == null) {
            callback.onError(new ApiException(UNAUTHORIZED, "", "Please sign in."));
            return;
        }
        action.accept(client.getSession().getUserId());
    }
}
