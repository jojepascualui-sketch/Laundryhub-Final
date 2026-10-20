package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.model.WalkInService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTimelineTest {

    private static Order order(ServiceType service, OrderStatus status) {
        return new Order.Builder().orderNo("LH-0001").serviceType(service).status(status).paymentMethod("CASH").build();
    }

    private static String states(List<OrderTimeline.Step> steps) {
        StringBuilder sb = new StringBuilder();
        for (OrderTimeline.Step s : steps) {
            sb.append(s.getState().name().charAt(0));
        }
        return sb.toString();
    }

    @Test
    void deliveryOutForDeliveryLikeTheMockup() {
        List<OrderTimeline.Step> steps = OrderTimeline.build(order(PickupDeliveryService.INSTANCE, OrderStatus.OUT_FOR_DELIVERY),
                Collections.emptyList());
        assertEquals(5, steps.size());
        assertEquals("DDDDC", states(steps));
        assertEquals("Delivered", steps.get(4).getLabel());
        assertEquals("Enjoy your fresh laundry", steps.get(4).getNote());
    }

    @Test
    void mockupStateWashingIsCurrentStep() {
        // Screen 07 shows: placed, picked up, washing done; out for delivery current (status READY).
        assertEquals("DDDCU", states(OrderTimeline.build(order(PickupDeliveryService.INSTANCE, OrderStatus.READY),
                Collections.emptyList())));
        assertEquals("DDCUU", states(OrderTimeline.build(order(PickupDeliveryService.INSTANCE, OrderStatus.WASHING),
                Collections.emptyList())));
    }

    @Test
    void pendingDeliveryUsesPickedUpEvent() {
        Order o = order(PickupDeliveryService.INSTANCE, OrderStatus.PENDING);
        assertEquals("DCUUU", states(OrderTimeline.build(o, Collections.emptyList())));
        List<StatusEvent> events = Arrays.asList(
                new StatusEvent("PLACED", OrderStatus.PENDING, null, "2026-10-20T02:00:00+00:00"),
                new StatusEvent("PICKED_UP", OrderStatus.PENDING, null, "2026-10-20T06:05:00+00:00"));
        List<OrderTimeline.Step> steps = OrderTimeline.build(o, events);
        assertEquals("DDCUU", states(steps));
        assertEquals("Oct 20, 2:05 PM", steps.get(1).getTime());
        assertEquals("Oct 20, 10:00 AM", steps.get(0).getTime());
    }

    @Test
    void completedAllDoneAndWalkInHasFourSteps() {
        assertEquals("DDDDD", states(OrderTimeline.build(order(PickupDeliveryService.INSTANCE, OrderStatus.COMPLETED),
                Collections.emptyList())));
        List<OrderTimeline.Step> walkIn = OrderTimeline.build(order(WalkInService.INSTANCE, OrderStatus.READY),
                Collections.emptyList());
        assertEquals("DDDC", states(walkIn));
        assertEquals("Ready for pickup", walkIn.get(2).getLabel());
    }

    @Test
    void cancelledHasNoTimeline() {
        assertTrue(OrderTimeline.build(order(WalkInService.INSTANCE, OrderStatus.CANCELLED), Collections.emptyList()).isEmpty());
    }
}
