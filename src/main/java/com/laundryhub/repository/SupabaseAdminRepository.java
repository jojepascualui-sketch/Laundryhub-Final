package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Role;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Callbacks;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;

/**
 * {@link AdminRepository} backed by Supabase (rpc functions from migration 0007).
 */
public final class SupabaseAdminRepository implements AdminRepository {

    private static final int FORBIDDEN = 403;

    private final SupabaseClient client;

    /**
     * @param client Supabase client
     */
    public SupabaseAdminRepository(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void loadDashboard(Callback<JsonObject> callback) {
        client.rpc("admin_dashboard", null, Callbacks.map(callback, r -> (JsonObject) r));
    }

    @Override
    public void loadReports(String fromDate, String toDate, Callback<JsonObject> callback) {
        client.rpc("admin_reports", new JsonObject().put("p_from", fromDate).put("p_to", toDate),
                Callbacks.map(callback, r -> (JsonObject) r));
    }

    @Override
    public void loadUsers(Role tab, Callback<JsonArray> callback) {
        client.rpc("admin_users", new JsonObject().put("p_role", tab.name()),
                Callbacks.map(callback, r -> r instanceof JsonArray ? (JsonArray) r : new JsonArray()));
    }

    @Override
    public void updateUser(String profileId, JsonObject changes, Callback<Void> callback) {
        client.update("profiles", Query.filter().eq("id", profileId), changes, requireRow(callback));
    }

    @Override
    public void findProfileByEmail(String email, Callback<JsonObject> callback) {
        client.selectOne("profiles", Query.select("*").eq("email", email.trim().toLowerCase()), callback);
    }

    @Override
    public void setRiderPlate(String riderId, String plateNo, Callback<Void> callback) {
        client.update("riders", Query.filter().eq("profile_id", riderId), new JsonObject().put("plate_no", plateNo),
                requireRow(callback));
    }

    private static Callback<JsonArray> requireRow(Callback<Void> callback) {
        return new Callback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray rows) {
                if (rows.isEmpty()) {
                    callback.onError(new ApiException(FORBIDDEN, "", "You don't have permission to change this account."));
                } else {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        };
    }
}
