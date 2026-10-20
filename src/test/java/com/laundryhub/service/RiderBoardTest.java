package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.laundryhub.model.Booking;
import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.RiderJob;
import com.laundryhub.util.TimeUtils;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class RiderBoardTest {

    private static final Location CUSTOMER = new Location(14.5935, 120.985, "Brgy. Poblacion");
    private static final Location SHOP = new Location(14.5896, 120.9811, "Shop");
    private static final long NOW = TimeUtils.parseIso("2026-10-20T06:00:00+00:00");

    private static Booking booking(long id, String type, JobStatus status, int step, String completedAt) {
        Order order = new Order.Builder().id(id).orderNo("LH-000" + id).serviceType(PickupDeliveryService.INSTANCE)
                .paymentMethod("CASH").location(CUSTOMER).build();
        return new Booking(RiderJob.create(type, id, id, status, step, "r1", true), order, completedAt);
    }

    @Test
    void countersAndLists() {
        RiderBoard board = new RiderBoard(
                Arrays.asList(booking(1, "PICKUP", JobStatus.AVAILABLE, 0, null)),
                Arrays.asList(
                        booking(2, "PICKUP", JobStatus.ACCEPTED, 0, null),
                        booking(3, "RETURN_DELIVERY", JobStatus.IN_PROGRESS, 2, null),
                        booking(4, "PICKUP", JobStatus.COMPLETED, 4, "2026-10-20T03:00:00+00:00"),
                        booking(5, "PICKUP", JobStatus.COMPLETED, 4, "2026-10-18T03:00:00+00:00")),
                NOW);
        assertEquals(1, board.getAvailableCount());
        assertEquals(2, board.getAcceptedCount());
        assertEquals(1, board.getDoneTodayCount());
        assertEquals(3, board.getMyJobs().size());
        assertEquals(3, board.getActiveJob().getJob().getId(), "in-progress job wins");
    }

    @Test
    void doneTodayUsesPhilippineDate() {
        // 17:30 UTC on Oct 19 is 01:30 on Oct 20 in Manila → counts as today.
        RiderBoard board = new RiderBoard(Collections.emptyList(),
                Arrays.asList(booking(4, "PICKUP", JobStatus.COMPLETED, 4, "2026-10-19T17:30:00+00:00")), NOW);
        assertEquals(1, board.getDoneTodayCount());
        assertNull(board.getActiveJob());
    }

    @Test
    void destinationFollowsTheStep() {
        assertEquals(CUSTOMER, RiderBoard.destination(booking(1, "PICKUP", JobStatus.ACCEPTED, 0, null), SHOP));
        assertEquals(SHOP, RiderBoard.destination(booking(1, "PICKUP", JobStatus.IN_PROGRESS, 3, null), SHOP));
        assertEquals(SHOP, RiderBoard.destination(booking(1, "RETURN_DELIVERY", JobStatus.ACCEPTED, 0, null), SHOP));
        assertEquals(CUSTOMER, RiderBoard.destination(booking(1, "RETURN_DELIVERY", JobStatus.IN_PROGRESS, 1, null), SHOP));
    }

    @Test
    void distanceAndDirections() {
        Booking b = booking(1, "PICKUP", JobStatus.AVAILABLE, 0, null);
        assertEquals("", RiderBoard.distanceLabel(b, null));
        assertEquals("0.6 km", RiderBoard.distanceLabel(b, SHOP));
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=14.5935,120.985", RiderBoard.directionsUrl(CUSTOMER));
    }
}
