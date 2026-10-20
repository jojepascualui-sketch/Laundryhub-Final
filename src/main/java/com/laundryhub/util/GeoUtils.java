package com.laundryhub.util;

/**
 * Distance on the earth's surface (Haversine), the same formula as the SQL
 * function distance_km, so the early "outside the delivery area" warning matches the server.
 */
public final class GeoUtils {

    /** Mean earth radius in km. */
    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * @param lat1 latitude of the first point (degrees)
     * @param lng1 longitude of the first point (degrees)
     * @param lat2 latitude of the second point (degrees)
     * @param lng2 longitude of the second point (degrees)
     * @return distance in kilometres
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Formats a distance like the rider cards: "1.2 km".
     *
     * @param km distance in kilometres
     * @return text with one decimal
     */
    public static String formatKm(double km) {
        long tenths = Math.round(km * 10);
        return (tenths / 10) + "." + (tenths % 10) + " km";
    }
}
