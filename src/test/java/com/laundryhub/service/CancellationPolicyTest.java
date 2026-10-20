package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.RiderJob;
import com.laundryhub.model.StatusEvent;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CancellationPolicyTest {

    private static Order order(OrderStatus status, String method, PaymentStatus payment) {
        return new Order.Builder().id(1).orderNo("LH-0001").serviceType(PickupDeliveryService.INSTANCE)
                .status(status).paymentMethod(method).paymentStatus(payment).build();
    }

    private static RiderJob job(String type, JobStatus status, int step) {
        return RiderJob.create(type, 7, 1, status, step, "rider-1", true);
    }

    @Test
    void customerCanCancelPendingOrderBeforePickup() {
        Order o = order(OrderStatus.PENDING, "CASH", PaymentStatus.UNPAID);
        assertTrue(CancellationPolicy.customerCanCancel(o, Collections.emptyList()));
        assertTrue(CancellationPolicy.customerCanCancel(o, null));
        assertTrue(CancellationPolicy.customerCanCancel(o, Arrays.asList(
                new StatusEvent("PENDING", OrderStatus.PENDING, "Order placed", null))));
    }

    @Test
    void customerCannotCancelAfterPickupOrOnceWashing() {
        Order pending = order(OrderStatus.PENDING, "CASH", PaymentStatus.UNPAID);
        assertFalse(CancellationPolicy.customerCanCancel(pending, Arrays.asList(
                new StatusEvent("PICKED_UP", OrderStatus.PENDING, "Rider picked up", null))));
        assertFalse(CancellationPolicy.customerCanCancel(order(OrderStatus.WASHING, "CASH", PaymentStatus.UNPAID), null));
        assertFalse(CancellationPolicy.customerCanCancel(order(OrderStatus.CANCELLED, "CASH", PaymentStatus.UNPAID), null));
        assertFalse(CancellationPolicy.customerCanCancel(null, null));
    }

    @Test
    void riderCanReleasePickupUntilConfirmPickup() {
        assertTrue(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_PICKUP, JobStatus.ACCEPTED, 0)));
        assertTrue(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_PICKUP, JobStatus.IN_PROGRESS, 2)));
        assertFalse(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_PICKUP, JobStatus.IN_PROGRESS, 3)));
    }

    @Test
    void riderCanReleaseDeliveryOnlyBeforeLeavingShop() {
        assertTrue(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_RETURN, JobStatus.ACCEPTED, 0)));
        assertFalse(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_RETURN, JobStatus.IN_PROGRESS, 1)));
    }

    @Test
    void finishedOrOpenJobsCannotBeReleased() {
        assertFalse(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_PICKUP, JobStatus.COMPLETED, 0)));
        assertFalse(CancellationPolicy.riderCanRelease(job(RiderJob.TYPE_PICKUP, JobStatus.AVAILABLE, 0)));
        assertFalse(CancellationPolicy.riderCanRelease(null));
    }

    @Test
    void refundNoteOnlyForPaidGcash() {
        assertEquals("You paid by GCash. The shop will contact you about your refund.",
                CancellationPolicy.refundNote(order(OrderStatus.CANCELLED, "GCASH", PaymentStatus.PAID)));
        assertNull(CancellationPolicy.refundNote(order(OrderStatus.CANCELLED, "GCASH", PaymentStatus.PENDING_VERIFICATION)));
        assertNull(CancellationPolicy.refundNote(order(OrderStatus.CANCELLED, "CASH", PaymentStatus.PAID)));
    }
}
