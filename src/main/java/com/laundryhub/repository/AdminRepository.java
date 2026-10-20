package com.laundryhub.repository;

import com.laundryhub.model.Role;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;

/**
 * Admin console data (screens A1, A4, A5). Every call is checked by the database.
 */
public interface AdminRepository {

    /** @param callback receives the rpc admin_dashboard JSON (counts, pipeline, recent) */
    void loadDashboard(Callback<JsonObject> callback);

    /**
     * @param fromDate first day, YYYY-MM-DD
     * @param toDate   last day, YYYY-MM-DD
     * @param callback receives the rpc admin_reports JSON
     */
    void loadReports(String fromDate, String toDate, Callback<JsonObject> callback);

    /**
     * @param tab      CUSTOMER, RIDER or STAFF (STAFF lists ADMIN + STAFF)
     * @param callback receives the users of that tab
     */
    void loadUsers(Role tab, Callback<JsonArray> callback);

    /**
     * Changes role, status or job title (ADMIN only).
     *
     * @param profileId profile id
     * @param changes   any of role, active, job_title, full_name, mobile
     * @param callback  called when saved
     */
    void updateUser(String profileId, JsonObject changes, Callback<Void> callback);

    /**
     * Finds an existing account by e-mail (used by "+ Add rider/staff": the person signs up first).
     *
     * @param email    e-mail
     * @param callback receives the profile row, or null when nobody uses that e-mail
     */
    void findProfileByEmail(String email, Callback<JsonObject> callback);

    /**
     * @param riderId  rider profile id
     * @param plateNo  plate number
     * @param callback called when saved
     */
    void setRiderPlate(String riderId, String plateNo, Callback<Void> callback);
}
