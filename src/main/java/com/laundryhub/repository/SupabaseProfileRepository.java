package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Callbacks;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.Validators;

/**
 * {@link ProfileRepository} backed by Supabase.
 * OOP: Inheritance/Polymorphism — implements the interface; swap it for a fake in tests.
 */
public final class SupabaseProfileRepository implements ProfileRepository {

    private static final int NOT_FOUND = 404;

    private final SupabaseClient client;

    /**
     * @param client Supabase client
     */
    public SupabaseProfileRepository(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void findUser(String userId, Callback<User> callback) {
        client.selectOne("profiles", Query.select("*").eq("id", userId), new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject profile) {
                if (profile == null) {
                    callback.onError(new ApiException(NOT_FOUND, "",
                            "Your profile was not found. Please contact the shop."));
                    return;
                }
                if (Role.fromCode(profile.getString("role")) != Role.RIDER) {
                    Callbacks.<JsonObject, User>map(callback, p -> ModelMapper.toUser(p, null)).onSuccess(profile);
                    return;
                }
                client.selectOne("riders", Query.select("*").eq("profile_id", userId),
                        Callbacks.map(callback, rider -> ModelMapper.toUser(profile, rider)));
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void updateOwnProfile(String userId, String fullName, String mobile, Callback<Void> callback) {
        JsonObject changes = new JsonObject()
                .put("full_name", fullName == null ? "" : fullName.trim())
                .put("mobile", Validators.hasText(mobile) ? Validators.compact(mobile) : null);
        client.update("profiles", Query.filter().eq("id", userId), changes, Callbacks.ignoreResult(callback));
    }
}
