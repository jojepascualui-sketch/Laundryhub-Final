package com.laundryhub.repository;

import com.laundryhub.model.Booking;
import com.laundryhub.model.Location;
import com.laundryhub.model.RiderJob;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import java.util.List;

/**
 * Rider jobs and the rider's own duty/GPS row. Accepting and advancing go through
 * database functions so two riders can never take the same booking.
 */
public interface RiderJobRepository {

    /** @param callback receives AVAILABLE bookings the rider has not declined (needs on duty) */
    void findAvailable(Callback<List<Booking>> callback);

    /** @param callback receives the rider's own jobs (accepted, in progress, completed today) */
    void findMine(Callback<List<Booking>> callback);

    /**
     * @param jobId    job id
     * @param callback receives the job with its order, or an error when not visible
     */
    void findJob(long jobId, Callback<Booking> callback);

    /**
     * @param jobId    job id
     * @param callback receives the accepted job; error 409 "This booking was already taken."
     */
    void accept(long jobId, Callback<RiderJob> callback);

    /**
     * Rider gives an accepted job back before taking the laundry (rpc release_job).
     * The job becomes available to other riders.
     *
     * @param jobId    job id
     * @param reason   reason chosen by the rider, may be null
     * @param callback called when released; error 409 when the laundry was already taken
     */
    void releaseJob(long jobId, String reason, Callback<Void> callback);

    /**
     * @param jobId    job id
     * @param callback called when hidden for this rider
     */
    void decline(long jobId, Callback<Void> callback);

    /**
     * Completes the next step.
     *
     * @param jobId    job id
     * @param callback receives the server's answer (job fields + done, order_status, payment_status)
     */
    void advance(long jobId, Callback<JsonObject> callback);

    /**
     * @param onDuty   new duty state
     * @param callback called when saved
     */
    void setOnDuty(boolean onDuty, Callback<Void> callback);

    /**
     * Sends the rider's GPS position (riders.last_lat/last_lng/last_seen).
     *
     * @param position current position
     * @param callback called when saved
     */
    void updateLocation(Location position, Callback<Void> callback);

    /**
     * Rider info for the customer's tracking page.
     *
     * @param riderId  rider profile id
     * @param callback receives {full_name, mobile, plate_no, last_lat, last_lng, last_seen}, or null
     */
    void findRiderForTracking(String riderId, Callback<JsonObject> callback);
}
