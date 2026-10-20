package com.laundryhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.repository.Query;
import org.junit.jupiter.api.Test;

class OrderLabelsTest {

    private static Order order(ServiceType service, String pay, PaymentStatus status) {
        return new Order.Builder().orderNo("LH-0001").serviceType(service).paymentMethod(pay).paymentStatus(status).build();
    }

    @Test
    void customerPaymentLabels() {
        assertEquals("GCash · Waiting for verification",
                order(WalkInService.INSTANCE, "GCASH", PaymentStatus.PENDING_VERIFICATION).getCustomerPaymentLabel());
        assertEquals("GCash · Rejected — please resend",
                order(WalkInService.INSTANCE, "GCASH", PaymentStatus.REJECTED).getCustomerPaymentLabel());
        assertEquals("GCash · Paid", order(WalkInService.INSTANCE, "GCASH", PaymentStatus.PAID).getCustomerPaymentLabel());
        assertEquals("Cash · Pay the rider upon delivery",
                order(PickupDeliveryService.INSTANCE, "CASH", PaymentStatus.UNPAID).getCustomerPaymentLabel());
        assertEquals("Cash · Pay at the counter", order(WalkInService.INSTANCE, "CASH", PaymentStatus.UNPAID).getCustomerPaymentLabel());
        assertEquals("Cash · Paid", order(WalkInService.INSTANCE, "CASH", PaymentStatus.PAID).getCustomerPaymentLabel());
    }

    @Test
    void orderNeedsNumberAndService() {
        assertThrows(ValidationException.class, () -> new Order.Builder().serviceType(WalkInService.INSTANCE).build());
        assertThrows(ValidationException.class, () -> new Order.Builder().orderNo("LH-1").build());
        assertTrue(order(PickupDeliveryService.INSTANCE, "CASH", null).isDelivery());
        assertEquals(PaymentStatus.UNPAID, order(WalkInService.INSTANCE, "CASH", null).getPaymentStatus());
    }

    @Test
    void doneNotesPerJobType() {
        assertEquals("Laundry dropped at shop", RiderJob.create("PICKUP", 1, 1, JobStatus.COMPLETED, 4, "r", true).getDoneNote());
        assertEquals("Cash collected", RiderJob.create("RETURN_DELIVERY", 1, 1, JobStatus.COMPLETED, 5, "r", true).getDoneNote());
        assertEquals("Paid via GCash", RiderJob.create("RETURN_DELIVERY", 1, 1, JobStatus.COMPLETED, 4, "r", false).getDoneNote());
    }

    @Test
    void dateRangeQuery() {
        assertEquals("created_at=gte.2026-10-01T00%3A00%3A00%2B08%3A00&created_at=lt.2026-10-20T16%3A00%3A00Z",
                Query.filter().gte("created_at", "2026-10-01T00:00:00+08:00").lt("created_at", "2026-10-20T16:00:00Z").build());
    }
}
