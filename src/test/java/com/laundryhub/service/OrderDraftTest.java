package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.GCashPayment;
import com.laundryhub.model.Location;
import com.laundryhub.model.OrderRequest;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.model.WalkInService;
import com.laundryhub.repository.CatalogRepository;
import com.laundryhub.util.Callback;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderDraftTest {

    private static final Soap HYPO = new Soap(2, "Hypoallergenic", "", 500, true);
    private static final Location SHOP = new Location(14.5896, 120.9811, "Shop");
    private static final ShopSettings SETTINGS = new ShopSettings(3000, 8, 6000, 5, SHOP, "", null, null, true, true);

    @Test
    void continueRulesPerStep() {
        OrderDraft d = new OrderDraft();
        assertFalse(d.isStepValid(OrderDraft.STEP_SERVICE, SETTINGS));
        d.setService(PickupDeliveryService.INSTANCE);
        assertTrue(d.isStepValid(OrderDraft.STEP_SERVICE, SETTINGS));

        assertFalse(d.isStepValid(OrderDraft.STEP_LOAD, SETTINGS));
        d.setSoap(HYPO);
        assertTrue(d.isStepValid(OrderDraft.STEP_LOAD, SETTINGS));

        d.setSlot("Today, 2–4 PM");
        assertFalse(d.isStepValid(OrderDraft.STEP_SCHEDULE, SETTINGS), "delivery needs a pin");
        d.setPin(new Location(15.5, 121.5, "Far"));
        assertFalse(d.isStepValid(OrderDraft.STEP_SCHEDULE, SETTINGS), "pin outside radius");
        d.setPin(new Location(14.5935, 120.985, "Near"));
        assertTrue(d.isStepValid(OrderDraft.STEP_SCHEDULE, SETTINGS));

        assertTrue(d.isStepValid(OrderDraft.STEP_REVIEW, SETTINGS), "cash needs nothing more");
        d.setPaymentCode("GCASH");
        assertFalse(d.isStepValid(OrderDraft.STEP_REVIEW, SETTINGS));
        d.setGcashReference("1234 5678 90123");
        assertTrue(d.isStepValid(OrderDraft.STEP_REVIEW, SETTINGS));
        d.setGcashNumber("0917");
        assertFalse(d.isStepValid(OrderDraft.STEP_REVIEW, SETTINGS), "bad GCash number");
        d.setGcashNumber("0917 123 4567");
        assertEquals(OrderDraft.TOTAL_STEPS + 1, d.firstIncompleteStep(SETTINGS));
    }

    @Test
    void walkInNeedsNoPinAndChangingServiceClearsSlot() {
        OrderDraft d = new OrderDraft();
        d.setService(PickupDeliveryService.INSTANCE);
        d.setPin(new Location(14.59, 120.98, "x"));
        d.setSlot("Today, 2–4 PM");
        d.setService(WalkInService.INSTANCE);
        assertNull(d.getSlot());
        assertNull(d.getPin());
        d.setSoap(HYPO);
        d.setSlot("Today, morning");
        assertTrue(d.isStepValid(OrderDraft.STEP_SCHEDULE, SETTINGS));
        assertEquals("Drop-off schedule", d.stepTitle(OrderDraft.STEP_SCHEDULE));
        assertEquals(OrderDraft.STEP_SERVICE, new OrderDraft().firstIncompleteStep(SETTINGS));
    }

    @Test
    void estimateMatchesDesign() {
        OrderDraft d = new OrderDraft();
        PriceCalculator calc = new PriceCalculator(SETTINGS);
        assertEquals(24000, d.estimateTotal(calc), "8 kg, nothing chosen");
        d.setKg(10);
        d.setService(PickupDeliveryService.INSTANCE);
        d.setSoap(HYPO);
        assertEquals(41000, d.estimateTotal(calc));
    }

    @Test
    void toRequestBuildsGcashPayment() {
        OrderDraft d = new OrderDraft();
        d.setService(WalkInService.INSTANCE);
        d.setSoap(HYPO);
        d.setSlot("Today, morning");
        d.setPaymentCode("GCASH");
        d.setGcashReference("1234567890123");
        OrderRequest r = d.toRequest();
        assertInstanceOf(GCashPayment.class, r.getPayment());
        assertEquals("1234567890123", ((GCashPayment) r.getPayment()).getReference());
        assertNull(r.getLocation());
        d.setGcashReference("12");
        assertThrows(ValidationException.class, d::toRequest);
        d.reset();
        assertNull(d.getService());
        assertEquals(8, d.getKg());
    }

    @Test
    void catalogCacheLoadsOnceForManyCallers() {
        int[] loads = {0};
        CatalogRepository repo = new CatalogRepository() {
            @Override
            public void loadSettings(Callback<ShopSettings> callback) {
                loads[0]++;
                callback.onSuccess(SETTINGS);
            }

            @Override
            public void saveSettings(ShopSettings settings, Callback<Void> callback) {
            }

            @Override
            public void findAvailableSoaps(Callback<List<Soap>> callback) {
                callback.onSuccess(Arrays.asList(HYPO));
            }

            @Override
            public void findAllSoaps(Callback<List<Soap>> callback) {
            }

            @Override
            public void saveSoap(Soap soap, Callback<Soap> callback) {
            }

            @Override
            public void uploadGcashQr(Object browserFile, String contentType, Callback<String> callback) {
            }

            @Override
            public void removeGcashQr(Callback<Void> callback) {
            }

            @Override
            public String gcashQrUrl(String path) {
                return path;
            }
        };
        CatalogCache cache = new CatalogCache(repo);
        CatalogCache.Catalog[] got = new CatalogCache.Catalog[2];
        cache.get(cb(c -> got[0] = c));
        cache.get(cb(c -> got[1] = c));
        assertEquals(1, loads[0]);
        assertEquals(HYPO, got[1].findSoap(2));
        cache.invalidate();
        cache.get(cb(c -> { }));
        assertEquals(2, loads[0]);
    }

    private static Callback<CatalogCache.Catalog> cb(java.util.function.Consumer<CatalogCache.Catalog> ok) {
        return new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog result) {
                ok.accept(result);
            }

            @Override
            public void onError(ApiException error) {
                throw new AssertionError(error.getMessage());
            }
        };
    }
}
