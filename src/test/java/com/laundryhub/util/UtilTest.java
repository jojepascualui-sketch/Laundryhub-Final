package com.laundryhub.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.LaundryHubException;
import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void parsesSupabaseRow() {
        JsonObject o = Json.parseObject(
                "{\"id\":1,\"order_no\":\"LH-0001\",\"total\":410.00,\"lat\":null,\"paid\":true,"
                + "\"soaps\":{\"name\":\"Hypoallergenic\"},\"tags\":[1,\"a\"],\"note\":\"line\\nbreak \\u20b1\"}");
        assertEquals(1, o.getLong("id", 0));
        assertEquals("LH-0001", o.getString("order_no"));
        assertEquals(41000, o.getCentavos("total"));
        assertTrue(o.isNull("lat"));
        assertNull(o.getDoubleOrNull("lat"));
        assertTrue(o.getBoolean("paid", false));
        assertEquals("Hypoallergenic", o.getObject("soaps").getString("name"));
        assertEquals(2, o.getArray("tags").size());
        assertEquals("line\nbreak ₱", o.getString("note"));
        assertEquals("1", o.getString("id"));
        assertEquals(0, o.getArray("missing").size());
    }

    @Test
    void roundTripsJson() {
        JsonObject o = new JsonObject().put("p_kg", 10).put("p_note", "a \"quoted\" ₱")
                .put("p_lat", 14.5935).put("p_null", null).put("arr", new JsonArray().add(true).add(2.0));
        String text = Json.stringify(o);
        assertEquals("{\"p_kg\":10,\"p_note\":\"a \\\"quoted\\\" ₱\",\"p_lat\":14.5935,\"p_null\":null,\"arr\":[true,2]}", text);
        assertEquals(o.toString(), Json.parseObject(text).toString());
    }

    @Test
    void parsesArraysAndScalars() {
        assertEquals(3, Json.parseArray(" [ {}, [], 1e2 ] ").size());
        assertEquals(Double.valueOf(-1.5), Json.parse("-1.5"));
        assertNull(Json.parse("null"));
    }

    @Test
    void rejectsBadJson() {
        assertThrows(LaundryHubException.class, () -> Json.parse("{\"a\":}"));
        assertThrows(LaundryHubException.class, () -> Json.parse("[1,2"));
        assertThrows(LaundryHubException.class, () -> Json.parse("{} x"));
        assertThrows(LaundryHubException.class, () -> Json.parseObject("[]"));
    }

    @Test
    void validators() {
        assertTrue(Validators.isMobile("0917 123 4567"));
        assertFalse(Validators.isMobile("0817 123 4567"));
        assertFalse(Validators.isMobile("0917123456"));
        assertTrue(Validators.isGcashReference("1234567890123"));
        assertFalse(Validators.isGcashReference("123456789012"));
        assertTrue(Validators.isEmail("ana@example.com"));
        assertFalse(Validators.isEmail("ana@example"));
        assertFalse(Validators.isEmail("a b@example.com"));
        assertTrue(Validators.isPassword("12345678"));
        assertFalse(Validators.isPassword("1234567"));
    }

    @Test
    void moneyShortAndConversions() {
        assertEquals("₱30", MoneyFormatter.formatShort(3000));
        assertEquals("₱7.50", MoneyFormatter.formatShort(750));
        assertEquals(41000, MoneyFormatter.toCentavos(410.0));
        assertEquals(51600, MoneyFormatter.toCentavos(515.999));
        assertEquals(410.0, MoneyFormatter.toPesos(41000));
    }

    @Test
    void geoDistanceAndFormat() {
        double km = GeoUtils.distanceKm(14.5896, 120.9811, 15.5, 121.5);
        assertTrue(km > 115 && km < 116);
        assertEquals("1.2 km", GeoUtils.formatKm(1.234));
        assertEquals("0.0 km", GeoUtils.formatKm(0));
    }

    @Test
    void textHelpers() {
        assertEquals("RM", TextUtils.initials("Rider  Mendoza"));
        assertEquals("?", TextUtils.initials(""));
        assertEquals("0917 123 1021", TextUtils.formatMobile("09171231021"));
        assertEquals("abc", TextUtils.formatMobile("abc"));
    }
}
