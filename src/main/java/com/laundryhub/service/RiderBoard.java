package com.laundryhub.service;

import com.laundryhub.model.Booking;
import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Location;
import com.laundryhub.model.RiderJob;
import com.laundryhub.util.GeoUtils;
import com.laundryhub.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * The rider's bookings screen logic (R1): counters, the "My jobs" list, the active job and where
 * to navigate next. Pure Java, unit tested.
 */
public final class RiderBoard {

    private final List<Booking> available;
    private final List<Booking> mine;
    private final String today;

    /**
     * @param available   AVAILABLE bookings the rider can accept
     * @param mine        the rider's own jobs (accepted, in progress, completed)
     * @param nowMillis   current time (for "Done today", Philippine date)
     */
    public RiderBoard(List<Booking> available, List<Booking> mine, long nowMillis) {
        this.available = available == null ? new ArrayList<>() : new ArrayList<>(available);
        this.mine = mine == null ? new ArrayList<>() : new ArrayList<>(mine);
        this.today = TimeUtils.manilaDate(nowMillis);
    }

    /** @return bookings to accept */
    public List<Booking> getAvailable() {
        return available;
    }

    /** @return accepted / in-progress jobs, then jobs completed today */
    public List<Booking> getMyJobs() {
        List<Booking> list = new ArrayList<>(getOpenJobs());
        for (Booking b : mine) {
            if (b.getJob().getStatus() == JobStatus.COMPLETED && isToday(b)) {
                list.add(b);
            }
        }
        return list;
    }

    /** @return accepted or in-progress jobs */
    public List<Booking> getOpenJobs() {
        List<Booking> list = new ArrayList<>();
        for (Booking b : mine) {
            if (b.getJob().getStatus().isActive()) {
                list.add(b);
            }
        }
        return list;
    }

    /** @return "Available" counter */
    public int getAvailableCount() {
        return available.size();
    }

    /** @return "Accepted" counter (open jobs) */
    public int getAcceptedCount() {
        return getOpenJobs().size();
    }

    /** @return "Done today" counter */
    public int getDoneTodayCount() {
        int n = 0;
        for (Booking b : mine) {
            if (b.getJob().getStatus() == JobStatus.COMPLETED && isToday(b)) {
                n++;
            }
        }
        return n;
    }

    /** @return the job to continue: in progress first, otherwise the oldest accepted; null when none */
    public Booking getActiveJob() {
        Booking accepted = null;
        for (Booking b : mine) {
            if (b.getJob().getStatus() == JobStatus.IN_PROGRESS) {
                return b;
            }
            if (b.getJob().getStatus() == JobStatus.ACCEPTED) {
                accepted = b;
            }
        }
        return accepted;
    }

    private boolean isToday(Booking b) {
        long t = TimeUtils.parseIso(b.getCompletedAt());
        return t != Long.MIN_VALUE && today.equals(TimeUtils.manilaDate(t));
    }

    /**
     * Where the rider is heading for the job's current step.
     * Pickup: customer, then the shop for the last step ("Drop off at shop").
     * Return delivery: the shop first ("Collect laundry at shop"), then the customer.
     *
     * @param booking job + order
     * @param shop    shop location, may be null
     * @return destination, may be null when unknown
     */
    public static Location destination(Booking booking, Location shop) {
        RiderJob job = booking.getJob();
        Location customer = booking.getOrder().getLocation();
        if (RiderJob.TYPE_PICKUP.equals(job.getType())) {
            return job.getCurrentStep() >= job.getTotalSteps() - 1 && shop != null ? shop : customer;
        }
        return job.getCurrentStep() == 0 && shop != null ? shop : customer;
    }

    /**
     * @param booking job + order
     * @param rider   rider's position, may be null
     * @return "1.2 km" from the rider to the customer's pin, or "" when unknown
     */
    public static String distanceLabel(Booking booking, Location rider) {
        double km = booking.distanceKmFrom(rider);
        return km < 0 ? "" : GeoUtils.formatKm(km);
    }

    /**
     * @param destination where to go
     * @return Google Maps directions link (opens the app on phones)
     */
    public static String directionsUrl(Location destination) {
        return "https://www.google.com/maps/dir/?api=1&destination=" + destination.getLat() + "," + destination.getLng();
    }
}
