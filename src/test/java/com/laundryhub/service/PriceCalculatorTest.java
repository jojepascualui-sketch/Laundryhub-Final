package com.laundryhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.LaundryLoad;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.model.WalkInService;
import com.laundryhub.util.MoneyFormatter;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private static final Soap STANDARD = new Soap(1, "Standard", "", 0, true);
    private static final Soap HYPOALLERGENIC = new Soap(2, "Hypoallergenic", "", 500, true);
    private static final Soap PREMIUM = new Soap(3, "Premium Scented", "", 800, true);
    private static final Soap BABY_SAFE = new Soap(4, "Baby-Safe", "", 600, true);

    private final PriceCalculator calc = new PriceCalculator(ShopSettings.defaults());

    @Test
    void tenKgHypoallergenicDeliveryIs410() {
        PriceBreakdown p = calc.quote(PickupDeliveryService.INSTANCE, new LaundryLoad(10, HYPOALLERGENIC));
        assertEquals("₱410.00", MoneyFormatter.format(p.getTotal()));
        assertEquals(30000, p.getLaundryCost());
        assertEquals(5000, p.getSoapCost());
        assertEquals(6000, p.getDeliveryFee());
    }

    @Test
    void twelveKgPremiumScentedDeliveryIs516() {
        PriceBreakdown p = calc.quote(PickupDeliveryService.INSTANCE, new LaundryLoad(12, PREMIUM));
        assertEquals("₱516.00", MoneyFormatter.format(p.getTotal()));
    }

    @Test
    void eightKgStandardWalkInIs240() {
        PriceBreakdown p = calc.quote(WalkInService.INSTANCE, new LaundryLoad(8, STANDARD));
        assertEquals("₱240.00", MoneyFormatter.format(p.getTotal()));
        assertEquals(0, p.getDeliveryFee());
    }

    @Test
    void sevenKgIsChargedAsEight() {
        PriceBreakdown p = calc.quote(WalkInService.INSTANCE, new LaundryLoad(7, STANDARD));
        assertEquals(8, p.getChargedKg());
        assertEquals("₱240.00", MoneyFormatter.format(p.getTotal()));
    }

    @Test
    void nineKgBabySafeWalkInIs324() {
        assertEquals(32400, calc.quote(WalkInService.INSTANCE, new LaundryLoad(9, BABY_SAFE)).getTotal());
    }

    @Test
    void usesShopSettingsNotConstants() {
        ShopSettings custom = new ShopSettings(3500, 5, 5000, 3, null, "", null, null, true, true);
        PriceBreakdown p = new PriceCalculator(custom).quote(PickupDeliveryService.INSTANCE, new LaundryLoad(4, STANDARD));
        assertEquals(5, p.getChargedKg());
        assertEquals(5 * 3500 + 5000, p.getTotal());
    }

    @Test
    void estimateWithoutSoapUsesMinimumAndFee() {
        assertEquals(24000 + 6000, calc.estimateTotal(PickupDeliveryService.INSTANCE, null, 6));
        assertEquals(24000, calc.estimateTotal(null, null, 8));
    }

    @Test
    void missingServiceIsRejected() {
        assertThrows(ValidationException.class, () -> calc.quote(null, new LaundryLoad(8, STANDARD)));
    }
}
