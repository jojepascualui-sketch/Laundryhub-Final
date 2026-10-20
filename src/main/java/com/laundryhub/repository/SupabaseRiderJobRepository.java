package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.LaundryHubException;
import com.laundryhub.model.Booking;
import com.laundryhub.model.Location;
import com.laundryhub.model.RiderJob;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Callbacks;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RiderJobRepository} backed by Supabase (rpc accept_job / decline_job / advance_job).
 */
public final class SupabaseRiderJobRepository implements RiderJobRepository {

    private static final String JOB_SELECT = "*,order:orders(" + ModelMapper.ORDER_SELECT + ")";
    private static final int MAX_JOBS = 100;

    private final SupabaseClient client;

    /**
     * @param client Supabase client
     */
    public SupabaseRiderJobRepository(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void findAvailable(Callback<List<Booking>> callback) {
        // RLS already hides declined jobs and returns nothing while off duty.
        client.select("rider_jobs", Query.select(JOB_SELECT).eq("status", "AVAILABLE").order("created_at", false).limit(MAX_JOBS),
                Callbacks.map(callback, SupabaseRiderJobRepository::toBookings));
    }

    @Override
    public void findMine(Callback<List<Booking>> callback) {
        AuthUser.require(client, callback, userId ->
                client.select("rider_jobs", Query.select(JOB_SELECT).eq("rider_id", userId)
                                .in("status", "ACCEPTED", "IN_PROGRESS", "COMPLETED").order("accepted_at", true).limit(MAX_JOBS),
                        Callbacks.map(callback, SupabaseRiderJobRepository::toBookings)));
    }

    @Override
    public void findJob(long jobId, Callback<Booking> callback) {
        client.selectOne("rider_jobs", Query.select(JOB_SELECT).eq("id", jobId), Callbacks.map(callback, row -> {
            if (row == null || row.getObject("order") == null) {
                throw new LaundryHubException("This job is not available to you.");
            }
            return ModelMapper.toBooking(row);
        }));
    }

    @Override
    public void accept(long jobId, Callback<RiderJob> callback) {
        client.rpc("accept_job", new JsonObject().put("p_job_id", jobId),
                Callbacks.map(callback, r -> ModelMapper.toJob((JsonObject) r, false)));
    }

    @Override
    public void decline(long jobId, Callback<Void> callback) {
        client.rpc("decline_job", new JsonObject().put("p_job_id", jobId), Callbacks.ignoreResult(callback));
    }

    @Override
    public void advance(long jobId, Callback<JsonObject> callback) {
        client.rpc("advance_job", new JsonObject().put("p_job_id", jobId), Callbacks.map(callback, r -> (JsonObject) r));
    }

    @Override
    public void setOnDuty(boolean onDuty, Callback<Void> callback) {
        updateOwnRider(new JsonObject().put("on_duty", onDuty), callback);
    }

    @Override
    public void updateLocation(Location position, Callback<Void> callback) {
        updateOwnRider(new JsonObject()
                .put("last_lat", position.getLat())
                .put("last_lng", position.getLng())
                .put("last_seen", TimeUtils.isoUtc(System.currentTimeMillis())), callback);
    }

    @Override
    public void findRiderForTracking(String riderId, Callback<JsonObject> callback) {
        client.selectOne("riders", Query.select("*,profile:profiles(full_name,mobile)").eq("profile_id", riderId),
                Callbacks.map(callback, row -> {
                    if (row == null) {
                        return null;
                    }
                    JsonObject profile = row.getObject("profile");
                    if (profile != null) {
                        row.put("full_name", profile.getString("full_name")).put("mobile", profile.getString("mobile"));
                    }
                    return row;
                }));
    }

    private void updateOwnRider(JsonObject changes, Callback<Void> callback) {
        AuthUser.require(client, callback, userId ->
                client.update("riders", Query.filter().eq("profile_id", userId), changes, new Callback<JsonArray>() {
                    @Override
                    public void onSuccess(JsonArray rows) {
                        if (rows.isEmpty()) {
                            callback.onError(new ApiException(403, "", "Your rider profile is not set up yet. Ask the admin."));
                        } else {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                }));
    }

    private static List<Booking> toBookings(JsonArray rows) {
        List<Booking> list = new ArrayList<>();
        for (JsonObject row : rows.objects()) {
            if (row.getObject("order") != null) {
                list.add(ModelMapper.toBooking(row));
            }
        }
        return list;
    }
}
