package com.laundryhub.ui;

import com.laundryhub.model.Location;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.util.GeoUtils;
import java.util.function.Consumer;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 04a map: tap the map to move the orange pin, drag the pin, or use GPS.
 * After each move the address is looked up (reverse geocoding) and the delivery radius is checked.
 * Without a Maps key (or when Maps fails) it shows the drawn map and still works with GPS.
 */
public class LocationPicker extends Component {

    /** Zoom for a street-level view. */
    private static final int STREET_ZOOM = 17;
    /** Zoom showing the whole neighbourhood. */
    private static final int AREA_ZOOM = 14;
    /** Fallback center when the shop has no pin: Manila. */
    private static final double DEFAULT_LAT = 14.5995;
    private static final double DEFAULT_LNG = 120.9842;

    private final ShopSettings settings;
    private final Consumer<Location> onChange;
    private Location pin;

    private HTMLElement mapBox;
    private HTMLElement chip;
    private HTMLElement fallbackPin;
    private HTMLElement addressLine;
    private HTMLElement areaWarning;
    private Button gpsButton;
    private GoogleMaps.GMap map;
    private GoogleMaps.Marker marker;
    private boolean usingGoogle;

    /**
     * @param settings shop settings (shop pin and delivery radius)
     * @param initial  pin chosen before, may be null
     * @param onChange called with every new pin (address filled in when known)
     */
    public LocationPicker(ShopSettings settings, Location initial, Consumer<Location> onChange) {
        this.settings = settings;
        this.pin = initial;
        this.onChange = onChange;
    }

    @Override
    protected HTMLElement render() {
        mapBox = Dom.div("map-box map-box--loading");
        mapBox.setAttribute("role", "application");
        mapBox.setAttribute("aria-label", "Map: tap to pin where our rider should pick up and return your laundry");
        chip = Dom.text("span", "map-box__chip", "");
        fallbackPin = Dom.icon(GoogleMaps.iconSvg(GoogleMaps.MarkerStyle.PIN), "map-box__pin");

        gpsButton = new Button("Use my current location", Button.Variant.OUTLINE, Icons.LOCATE, false, this::useGps)
                .block(true);
        addressLine = Dom.text("p", "map-address", "");
        areaWarning = Dom.div("note note--warn", Dom.icon(Icons.ALERT, null), Dom.text("span", null, ""));

        HTMLElement root = Dom.div("stack", mapBox, gpsButton.getElement(), addressLine, areaWarning);
        refreshTexts();
        // Start after the element is in the page (the map needs its size).
        Window.setTimeout(() -> GoogleMaps.whenReady(this::initGoogleMap, this::initFallback), 0);
        return root;
    }

    private void initGoogleMap() {
        usingGoogle = true;
        Dom.clear(mapBox);
        mapBox.getClassList().remove("map-box--loading");
        HTMLElement canvas = Dom.div("map-box__canvas");
        Dom.append(mapBox, canvas, chip);

        Location center = pin != null ? pin : settings.getShopLocation();
        double lat = center != null ? center.getLat() : DEFAULT_LAT;
        double lng = center != null ? center.getLng() : DEFAULT_LNG;
        map = GoogleMaps.createMap(canvas, lat, lng, pin != null ? STREET_ZOOM : AREA_ZOOM);

        Location shop = settings.getShopLocation();
        if (shop != null) {
            GoogleMaps.createMarker(map, shop.getLat(), shop.getLng(), GoogleMaps.MarkerStyle.SHOP, false, "Laundry HUB");
            GoogleMaps.createAreaCircle(map, shop.getLat(), shop.getLng(), settings.getDeliveryRadiusKm());
        }
        GoogleMaps.onMapClick(map, p -> movePin(p.lat(), p.lng(), false));
        GoogleMaps.watchAuthFailure(() -> {
            map = null;
            marker = null;
            initFallback();
        });
        if (pin != null) {
            ensureMarker(pin.getLat(), pin.getLng());
        }
        refreshTexts();
    }

    private void initFallback() {
        usingGoogle = false;
        Dom.clear(mapBox);
        mapBox.getClassList().remove("map-box--loading");
        mapBox.getClassList().add("map-box--static");
        HTMLElement art = Dom.div("map-box__art");
        art.setInnerHTML(MapArt.SVG);
        Dom.append(mapBox, art, fallbackPin, chip);
        mapBox.setAttribute("tabindex", "0");
        Dom.onClick(mapBox, this::useGps);
        refreshTexts();
    }

    private void ensureMarker(double lat, double lng) {
        if (marker == null) {
            marker = GoogleMaps.createMarker(map, lat, lng, GoogleMaps.MarkerStyle.PIN, true, "Your pin");
            GoogleMaps.onMarkerDragEnd(marker, p -> movePin(p.lat(), p.lng(), false));
        } else {
            marker.setPosition(GoogleMaps.latLng(lat, lng));
        }
    }

    /**
     * Moves the pin, centers the map and looks up the address.
     *
     * @param lat      latitude
     * @param lng      longitude
     * @param zoomIn   true to zoom to street level (after GPS)
     */
    private void movePin(double lat, double lng, boolean zoomIn) {
        pin = new Location(lat, lng, "");
        if (usingGoogle) {
            ensureMarker(lat, lng);
            map.panTo(GoogleMaps.latLng(lat, lng));
            if (zoomIn && map.getZoom() < STREET_ZOOM) {
                map.setZoom(STREET_ZOOM);
            }
            addressLine.setTextContent("Finding the address…");
            Location asked = pin;
            GoogleMaps.reverseGeocode(lat, lng, (address, status) -> {
                if (pin != asked) {
                    return; // the pin moved again meanwhile
                }
                pin = pin.withAddress(address);
                refreshTexts();
                onChange.accept(pin);
            });
        }
        refreshTexts();
        onChange.accept(pin);
    }

    private void useGps() {
        gpsButton.setLoading(true);
        GeoLocator.locate(new GeoLocator.Listener() {
            @Override
            public void onPosition(double lat, double lng) {
                gpsButton.setLoading(false);
                movePin(lat, lng, true);
            }

            @Override
            public void onError(String message) {
                gpsButton.setLoading(false);
                Toast.error(message);
            }
        });
    }

    private void refreshTexts() {
        if (chip == null) {
            return;
        }
        chip.setTextContent(pin == null ? (usingGoogle ? "Tap anywhere to pin" : "Tap to use your location")
                : "Location pinned");
        fallbackPin.setHidden(pin == null);
        if (pin == null) {
            addressLine.setTextContent(usingGoogle || mapBox.getClassList().contains("map-box--loading") ? ""
                    : "The map is not available right now. Use your current location to pin.");
        } else if (!pin.getAddress().isEmpty()) {
            addressLine.setTextContent(pin.getAddress());
        } else if (!usingGoogle) {
            addressLine.setTextContent("Pinned at " + pin.formatCoordinates());
        }
        boolean outside = pin != null && !settings.isWithinDeliveryArea(pin);
        areaWarning.setHidden(!outside);
        if (outside) {
            double km = settings.getShopLocation().distanceKmTo(pin);
            double radius = settings.getDeliveryRadiusKm();
            String radiusText = radius == Math.rint(radius) ? Long.toString((long) radius) : Double.toString(radius);
            areaWarning.querySelector("span").setTextContent("Your pin is " + GeoUtils.formatKm(km)
                    + " from the shop. We deliver within " + radiusText
                    + " km, so please choose Walk-in or pin a closer address.");
        }
    }
}
