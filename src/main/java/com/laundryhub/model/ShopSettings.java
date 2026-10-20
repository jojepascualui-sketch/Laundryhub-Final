package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;

/**
 * Shop-wide settings (the single shop_settings row): prices, delivery area, GCash details.
 * Money is in centavos.
 */
public final class ShopSettings {

    /** The shop's GCash InstaPay (QR Ph) code that ships with the website. */
    public static final String BUILT_IN_GCASH_QR = "img/gcash-qr.jpg";
    /** Default price per kg: ₱20 (The Laundry Hub Iloilo; the design's sample was ₱30). */
    public static final long DEFAULT_RATE_PER_KG = 2000;
    /** Default minimum load. */
    public static final int DEFAULT_MIN_KG = 8;
    /** Default delivery fee: ₱60. */
    public static final long DEFAULT_DELIVERY_FEE = 6000;
    /** Default delivery radius in km. */
    public static final double DEFAULT_RADIUS_KM = 5;

    private final long ratePerKg;
    private final int minKg;
    private final long deliveryFee;
    private final double deliveryRadiusKm;
    private final Location shopLocation;
    private final String shopHours;
    private final String gcashNumber;
    private final String gcashAccountName;
    private final boolean cashEnabled;
    private final boolean gcashEnabled;
    private final String gcashQrPath;

    /**
     * @param ratePerKg        price per kg in centavos (&gt; 0)
     * @param minKg            minimum charged kg (&gt; 0)
     * @param deliveryFee      delivery fee in centavos (&ge; 0)
     * @param deliveryRadiusKm delivery radius in km (&gt; 0)
     * @param shopLocation     shop pin and address, may be null
     * @param shopHours        opening hours text
     * @param gcashNumber      shop GCash number, may be null
     * @param gcashAccountName GCash account name, may be null
     * @param cashEnabled      cash accepted
     * @param gcashEnabled     GCash accepted
     */
    public ShopSettings(long ratePerKg, int minKg, long deliveryFee, double deliveryRadiusKm,
                        Location shopLocation, String shopHours, String gcashNumber,
                        String gcashAccountName, boolean cashEnabled, boolean gcashEnabled) {
        this(ratePerKg, minKg, deliveryFee, deliveryRadiusKm, shopLocation, shopHours, gcashNumber,
                gcashAccountName, cashEnabled, gcashEnabled, null);
    }

    /**
     * Same as the 10-argument constructor, plus the shop's GCash QR Ph image.
     *
     * @param ratePerKg        price per kg in centavos (&gt; 0)
     * @param minKg            minimum charged kg (&gt; 0)
     * @param deliveryFee      delivery fee in centavos (&ge; 0)
     * @param deliveryRadiusKm delivery radius in km (&gt; 0)
     * @param shopLocation     shop pin and address, may be null
     * @param shopHours        opening hours text
     * @param gcashNumber      shop GCash number, may be null
     * @param gcashAccountName GCash account name, may be null
     * @param cashEnabled      cash accepted
     * @param gcashEnabled     GCash accepted
     * @param gcashQrPath      storage path of the QR Ph image (bucket shop-public), may be null
     */
    public ShopSettings(long ratePerKg, int minKg, long deliveryFee, double deliveryRadiusKm,
                        Location shopLocation, String shopHours, String gcashNumber,
                        String gcashAccountName, boolean cashEnabled, boolean gcashEnabled, String gcashQrPath) {
        if (ratePerKg <= 0) {
            throw new ValidationException("Price per kilo must be more than zero.");
        }
        if (minKg <= 0) {
            throw new ValidationException("Minimum load must be at least 1 kg.");
        }
        if (deliveryFee < 0) {
            throw new ValidationException("Delivery fee cannot be negative.");
        }
        if (deliveryRadiusKm <= 0) {
            throw new ValidationException("Delivery radius must be more than zero.");
        }
        this.ratePerKg = ratePerKg;
        this.minKg = minKg;
        this.deliveryFee = deliveryFee;
        this.deliveryRadiusKm = deliveryRadiusKm;
        this.shopLocation = shopLocation;
        this.shopHours = shopHours == null ? "" : shopHours;
        this.gcashNumber = gcashNumber;
        this.gcashAccountName = gcashAccountName;
        this.cashEnabled = cashEnabled;
        this.gcashEnabled = gcashEnabled;
        this.gcashQrPath = gcashQrPath == null || gcashQrPath.trim().isEmpty() ? null : gcashQrPath;
    }

    /** @return the shop defaults (₱20/kg, 8 kg, ₱60, 5 km) */
    public static ShopSettings defaults() {
        return new ShopSettings(DEFAULT_RATE_PER_KG, DEFAULT_MIN_KG, DEFAULT_DELIVERY_FEE, DEFAULT_RADIUS_KM,
                null, "", null, null, true, true);
    }

    /** @return price per kg in centavos */
    public long getRatePerKg() {
        return ratePerKg;
    }

    /** @return minimum charged kg */
    public int getMinKg() {
        return minKg;
    }

    /** @return delivery fee in centavos */
    public long getDeliveryFee() {
        return deliveryFee;
    }

    /** @return delivery radius in km */
    public double getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    /** @return shop pin and address, may be null */
    public Location getShopLocation() {
        return shopLocation;
    }

    /** @return shop address text, never null */
    public String getShopAddress() {
        return shopLocation == null ? "" : shopLocation.getAddress();
    }

    /** @return opening hours text */
    public String getShopHours() {
        return shopHours;
    }

    /** @return shop GCash number, may be null */
    public String getGcashNumber() {
        return gcashNumber;
    }

    /** @return GCash account name, may be null */
    public String getGcashAccountName() {
        return gcashAccountName;
    }

    /** @return true when cash is accepted */
    public boolean isCashEnabled() {
        return cashEnabled;
    }

    /** @return storage path of the GCash QR Ph image, or null when none was uploaded */
    public String getGcashQrPath() {
        return gcashQrPath;
    }

    /** @return true when GCash is accepted */
    public boolean isGcashEnabled() {
        return gcashEnabled;
    }

    /**
     * @param pin a customer pin
     * @return true when the pin is inside the delivery radius (true when the shop has no pin)
     */
    public boolean isWithinDeliveryArea(Location pin) {
        return shopLocation == null || pin == null || shopLocation.distanceKmTo(pin) <= deliveryRadiusKm;
    }
}
