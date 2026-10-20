package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.WalkInService;
import com.laundryhub.util.TimeUtils;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminServicesTest {

    private static Order order(String no, String customer, OrderStatus status, boolean delivery, long total) {
        return new Order.Builder().orderNo(no).customerName(customer).customerMobile("09170001021")
                .serviceType(delivery ? PickupDeliveryService.INSTANCE : WalkInService.INSTANCE)
                .status(status).paymentMethod("GCASH").paymentStatus(PaymentStatus.PAID).kg(10).soapName("Hypoallergenic")
                .laundryCost(30000).soapCost(5000).deliveryFee(delivery ? 6000 : 0).total(total)
                .location(delivery ? new Location(14.59, 120.98, "Brgy. San Isidro") : null)
                .createdAt("2026-10-20T02:00:00+00:00").build();
    }

    private static final List<Order> ORDERS = Arrays.asList(
            order("LH-0001", "Maria Santos", OrderStatus.OUT_FOR_DELIVERY, true, 41000),
            order("LH-0002", "Juan Dela Cruz", OrderStatus.WASHING, false, 24000),
            order("LH-0003", "Ana Reyes", OrderStatus.PENDING, true, 51600),
            order("LH-0006", "Paolo Tan", OrderStatus.PENDING, true, 58500));

    @Test
    void chipCountsLikeA2() {
        AdminOrderFilter f = new AdminOrderFilter(ORDERS);
        assertEquals(4, f.total());
        assertEquals(2, f.countByStatus().get(OrderStatus.PENDING));
        assertEquals(1, f.countByStatus().get(OrderStatus.WASHING));
        assertEquals(0, f.countByStatus().get(OrderStatus.COMPLETED));
    }

    @Test
    void filterByStatusAndSearch() {
        AdminOrderFilter f = new AdminOrderFilter(ORDERS);
        assertEquals(2, f.filter(OrderStatus.PENDING, null).size());
        assertEquals("LH-0003", f.filter(null, "ana").get(0).getOrderNo());
        assertEquals(1, f.filter(null, "lh-0006").size());
        assertEquals(3, f.filter(null, "san isidro").size());
        assertEquals(4, f.filter(null, "0917 000 1021").size(), "mobile with spaces");
        assertEquals(0, f.filter(OrderStatus.WASHING, "maria").size());
    }

    @Test
    void csvHasHeaderAndPlainAmounts() {
        String csv = CsvExporter.ordersToCsv(ORDERS.subList(0, 1));
        String[] lines = csv.substring(1).split("\r\n");
        assertEquals("Order no,Date,Customer,Service,Kg,Fabric soap,Payment,Payment status,Order status,Laundry,"
                + "Soap add-on,Delivery fee,Total", lines[0]);
        assertEquals("LH-0001,\"Oct 20, 10:00 AM\",Maria Santos,Pickup & Delivery,10,Hypoallergenic,GCash,PAID,"
                + "Out for delivery,300.00,50.00,60.00,410.00", lines[1]);
        assertTrue(csv.startsWith("﻿"));
    }

    @Test
    void csvEscaping() {
        assertEquals("\"Dela Cruz, Juan\"", CsvExporter.escape("Dela Cruz, Juan"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
        assertEquals("'=SUM(A1)", CsvExporter.escape("=SUM(A1)"));
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    void reportPeriods() {
        long now = TimeUtils.parseIso("2026-10-20T06:00:00+00:00");
        assertEquals("2026-10-20", ReportPeriod.TODAY.from(now));
        assertEquals("2026-10-14", ReportPeriod.LAST_7_DAYS.from(now));
        assertEquals("2026-09-21", ReportPeriod.LAST_30_DAYS.from(now));
        assertEquals("2026-10-01", ReportPeriod.THIS_MONTH.from(now));
        assertEquals("2026-10-20", ReportPeriod.THIS_MONTH.to(now));
    }
}
