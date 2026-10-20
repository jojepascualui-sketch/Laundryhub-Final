package com.laundryhub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.model.Booking;
import com.laundryhub.model.DeliveryJob;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.Rider;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Staff;
import com.laundryhub.model.User;
import com.laundryhub.util.Json;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.TimeUtils;
import org.junit.jupiter.api.Test;

class ModelMapperTest {

    private static final String ORDER_ROW = "{\"id\":1,\"order_no\":\"LH-0001\",\"customer_id\":\"c1\","
            + "\"service_type\":\"PICKUP_DELIVERY\",\"kg\":10,\"soap_id\":2,\"soap_name\":\"Hypoallergenic\","
            + "\"time_slot\":\"Today, 2–4 PM\",\"lat\":14.5935,\"lng\":120.985,\"address\":\"Brgy. San Isidro\","
            + "\"payment_method\":\"GCASH\",\"payment_status\":\"PAID\",\"rate_per_kg\":30.00,"
            + "\"soap_add_on_per_kg\":5.00,\"laundry_cost\":300.00,\"soap_cost\":50.00,\"delivery_fee\":60.00,"
            + "\"total\":410.00,\"status\":\"OUT_FOR_DELIVERY\",\"rider_id\":null,"
            + "\"customer\":{\"full_name\":\"Maria Santos\",\"mobile\":\"09170001021\"}}";

    @Test
    void mapsOrderRow() {
        Order o = ModelMapper.toOrder(Json.parseObject(ORDER_ROW));
        assertEquals("LH-0001", o.getOrderNo());
        assertEquals(PickupDeliveryService.INSTANCE, o.getServiceType());
        assertEquals(41000, o.getTotal());
        assertEquals(PaymentStatus.PAID, o.getPaymentStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, o.getStatus());
        assertEquals("Maria Santos", o.getCustomerName());
        assertEquals("Brgy. San Isidro", o.getLocation().getAddress());
        assertEquals("GCash", o.getPaymentLabel());
        assertNull(o.getRiderId());
        assertEquals(41000, o.getPriceBreakdown().getTotal());
    }

    @Test
    void mapsBookingWithCashSteps() {
        JsonObject row = Json.parseObject("{\"id\":9,\"order_id\":1,\"type\":\"RETURN_DELIVERY\",\"status\":\"AVAILABLE\","
                + "\"current_step\":0,\"order\":" + ORDER_ROW.replace("\"GCASH\"", "\"CASH\"") + "}");
        Booking b = ModelMapper.toBooking(row);
        assertInstanceOf(DeliveryJob.class, b.getJob());
        assertEquals(5, b.getJob().getTotalSteps());
        assertTrue(b.distanceKmFrom(new com.laundryhub.model.Location(14.5935, 120.985, "")) < 0.01);
    }

    @Test
    void mapsUsersAndSettings() {
        User rider = ModelMapper.toUser(Json.parseObject("{\"id\":\"r\",\"full_name\":\"Rider Mendoza\",\"role\":\"RIDER\"}"),
                Json.parseObject("{\"plate_no\":\"NAB 1234\",\"on_duty\":true}"));
        assertInstanceOf(Rider.class, rider);
        assertEquals("NAB 1234", ((Rider) rider).getPlateNo());
        assertTrue(((Rider) rider).isOnDuty());
        assertInstanceOf(Staff.class, ModelMapper.toUser(Json.parseObject("{\"id\":\"s\",\"role\":\"STAFF\"}"), null));

        ShopSettings s = ModelMapper.toSettings(Json.parseObject("{\"rate_per_kg\":30.00,\"min_kg\":8,\"delivery_fee\":60,"
                + "\"delivery_radius_km\":5.00,\"shop_lat\":14.5896,\"shop_lng\":120.9811,\"shop_address\":\"[SHOP ADDRESS]\","
                + "\"shop_hours\":\"8:00 AM – 8:00 PM\",\"gcash_number\":null,\"cash_enabled\":true,\"gcash_enabled\":false}"));
        assertEquals(3000, s.getRatePerKg());
        assertEquals("[SHOP ADDRESS]", s.getShopAddress());
        assertEquals(false, s.isGcashEnabled());
        JsonObject back = ModelMapper.fromSettings(s);
        assertEquals(30.0, back.getDouble("rate_per_kg", 0));
        assertEquals(14.5896, back.getDouble("shop_lat", 0));
        assertEquals(null, s.getGcashQrPath());
        assertFalse(back.has("gcash_qr_path"), "the QR is saved by its own upload, not by Save changes");
        ShopSettings withQr = ModelMapper.toSettings(Json.parseObject("{\"rate_per_kg\":20,\"gcash_number\":\"09242850309\","
                + "\"gcash_qr_path\":\"gcash-qr-1.png\"}"));
        assertEquals("gcash-qr-1.png", withQr.getGcashQrPath());
    }

    @Test
    void timeUtils() {
        assertEquals("1970-01-01T00:00:00Z", TimeUtils.isoUtc(0));
        long t = TimeUtils.parseIso("2026-10-20T17:30:00+00:00");
        assertEquals("2026-10-20T17:30:00Z", TimeUtils.isoUtc(t));
        assertEquals("2026-10-21", TimeUtils.manilaDate(t));
        assertEquals("Oct 21, 1:30 AM", TimeUtils.manilaShortLabel("2026-10-20T17:30:00.123456+00:00"));
        assertEquals("Oct 20, 2:05 PM", TimeUtils.manilaShortLabel("2026-10-20T14:05:00+08:00"));
        assertEquals("2024-02-29", TimeUtils.manilaDate(TimeUtils.parseIso("2024-02-28T16:00:00Z")));
        assertEquals("2026-10-19", TimeUtils.manilaDatePlusDays(t, -2));
        assertEquals(Long.MIN_VALUE, TimeUtils.parseIso("nope"));
    }
}
