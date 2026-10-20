package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.util.GeoUtils;

/**
 * A point on the map with its address text.
 */
public final class Location {

    private static final double MAX_LAT = 90;
    private static final double MAX_LNG = 180;

    private final double lat;
    private final double lng;
    private final String address;

    /**
     * @param lat     latitude in degrees (-90..90)
     * @param lng     longitude in degrees (-180..180)
     * @param address address text, may be null
     */
    public Location(double lat, double lng, String address) {
        if (Double.isNaN(lat) || lat < -MAX_LAT || lat > MAX_LAT) {
            throw new ValidationException("The pinned location is not valid.");
        }
        if (Double.isNaN(lng) || lng < -MAX_LNG || lng > MAX_LNG) {
            throw new ValidationException("The pinned location is not valid.");
        }
        this.lat = lat;
        this.lng = lng;
        this.address = address == null ? "" : address.trim();
    }

    /** @return latitude */
    public double getLat() {
        return lat;
    }

    /** @return longitude */
    public double getLng() {
        return lng;
    }

    /** @return address text, never null */
    public String getAddress() {
        return address;
    }

    /**
     * @param newAddress address text
     * @return a copy with another address
     */
    public Location withAddress(String newAddress) {
        return new Location(lat, lng, newAddress);
    }

    /**
     * @param other another point
     * @return distance in km (Haversine)
     */
    public double distanceKmTo(Location other) {
        return GeoUtils.distanceKm(lat, lng, other.lat, other.lng);
    }

    /** @return "lat, lng" with 5 decimals, as shown in the admin order panel */
    public String formatCoordinates() {
        return round5(lat) + ", " + round5(lng);
    }

    private static String round5(double v) {
        final double scale = 100000.0;
        return Double.toString(Math.round(v * scale) / scale);
    }
}
