package com.laundryhub.ui;

import com.laundryhub.model.Location;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.util.GeoUtils;
import com.laundryhub.util.Json;
import com.laundryhub.util.JsonObject;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Screen 04a pickup-point picker, in the style of ride-hailing apps (e.g. Maxim / Grab):
 * a pin fixed in the middle of the map — the customer drags the map under it — with the live
 * address of the point under the pin, an address search box and a round "my location" button.
 * Opens on the customer's GPS position when no pin was chosen yet.
 * Without a Maps key (or when Maps fails) it shows the drawn map and still works with GPS.
 */
public class LocationPicker extends Component {

    /** Zoom for a street-level view. */
    private static final int STREET_ZOOM = 17;
    /** Zoom showing the neighbourhood. */
    private static final int AREA_ZOOM = 15;
    /** Fallback center when the shop has no pin: The Laundry Hub Iloilo (Cubay, Jaro). */
    private static final double DEFAULT_LAT = 10.732437;
    private static final double DEFAULT_LNG = 122.560391;
    /** Wait after typing before searching. */
    private static final int SEARCH_DELAY_MILLIS = 450;
    private static final int MIN_SEARCH_CHARS = 3;

    private final ShopSettings settings;
    private final Consumer<Location> onChange;
    private Location pin;

    private HTMLElement mapBox;
    private HTMLElement centerPin;
    private HTMLElement locateButton;
    private HTMLElement addressTitle;
    private HTMLElement addressText;
    private HTMLElement areaWarning;
    private HTMLElement searchBox;
    private HTMLInputElement searchInput;
    private HTMLElement results;
    private GoogleMaps.GMap map;
    private boolean usingGoogle;
    private boolean userMoved;
    private int searchTimer = -1;
    private int geocodeSeq;

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
        // Search
        searchInput = Dom.input("search", "picker__search-input");
        searchInput.setPlaceholder("Search street, barangay or landmark");
        searchInput.setAttribute("aria-label", "Search for your address");
        searchInput.setAttribute("autocomplete", "off");
        searchInput.addEventListener("input", e -> scheduleSearch());
        searchInput.addEventListener("keydown", e -> {
            if ("Enter".equals(keyOf(e))) {
                e.preventDefault();
                runSearch();
            }
        });
        results = Dom.el("ul", "picker__results");
        results.setAttribute("role", "listbox");
        results.setHidden(true);
        searchBox = Dom.div("picker__search", Dom.icon(SEARCH_ICON, "picker__search-icon"), searchInput, results);
        searchBox.setHidden(true);

        // Map with the fixed center pin and the locate button
        mapBox = Dom.div("picker__map map-box--loading");
        mapBox.setAttribute("role", "application");
        mapBox.setAttribute("aria-label", "Map: move the map so the pin is on your pickup point");
        centerPin = Dom.div("picker__pin",
                Dom.icon(GoogleMaps.iconSvg(GoogleMaps.MarkerStyle.PIN), "picker__pin-icon"),
                Dom.el("span", "picker__pin-shadow"));
        locateButton = Dom.el("button", "picker__locate");
        locateButton.setAttribute("type", "button");
        locateButton.setAttribute("aria-label", "Use my current location");
        locateButton.appendChild(Dom.icon(Icons.LOCATE, null));
        Dom.onClick(locateButton, this::useGps);

        // Address card under the map
        addressTitle = Dom.text("span", "picker__card-label", "Pickup point");
        addressText = Dom.text("span", "picker__card-address", "");
        HTMLElement card = Dom.div("picker__card", Dom.icon(Icons.PIN, "picker__card-icon"),
                Dom.div("picker__card-text", addressTitle, addressText));
        areaWarning = Dom.div("note note--warn", Dom.icon(Icons.ALERT, null), Dom.text("span", null, ""));

        HTMLElement root = Dom.div("picker", searchBox, mapBox, card, areaWarning);
        refreshTexts();
        // Start after the element is in the page (the map needs its size).
        Window.setTimeout(() -> GoogleMaps.whenReady(this::initGoogleMap, this::initFallback), 0);
        return root;
    }

    // ---- Google map ---------------------------------------------------------------------

    private void initGoogleMap() {
        usingGoogle = true;
        Dom.clear(mapBox);
        mapBox.getClassList().remove("map-box--loading");
        HTMLElement canvas = Dom.div("picker__canvas");
        Dom.append(mapBox, canvas, centerPin, locateButton);
        searchBox.setHidden(false);

        Location start = pin != null ? pin : settings.getShopLocation();
        double lat = start != null ? start.getLat() : DEFAULT_LAT;
        double lng = start != null ? start.getLng() : DEFAULT_LNG;
        map = GoogleMaps.createPickerMap(canvas, lat, lng, pin != null ? STREET_ZOOM : AREA_ZOOM);
        userMoved = pin != null;

        Location shop = settings.getShopLocation();
        if (shop != null) {
            GoogleMaps.createMarker(map, shop.getLat(), shop.getLng(), GoogleMaps.MarkerStyle.SHOP, false, "Laundry HUB");
            GoogleMaps.createAreaCircle(map, shop.getLat(), shop.getLng(), settings.getDeliveryRadiusKm());
        }
        GoogleMaps.onMapDragStart(map, () -> {
            userMoved = true;
            hideResults();
            Dom.toggleClass(centerPin, "is-moving", true);
            addressText.setTextContent("Release to set the pickup point");
        });
        // Tapping the map moves that point under the pin.
        GoogleMaps.onMapClick(map, p -> {
            userMoved = true;
            hideResults();
            map.panTo(p);
        });
        GoogleMaps.onMapIdle(map, center -> {
            Dom.toggleClass(centerPin, "is-moving", false);
            if (userMoved) {
                setPin(center.lat(), center.lng());
            }
        });
        GoogleMaps.watchAuthFailure(() -> {
            map = null;
            initFallback();
        });
        refreshTexts();
        if (pin == null) {
            useGps(); // open on the customer's position, like ride-hailing apps
        }
    }

    /** Pin = point under the fixed center pin; then look up its address. */
    private void setPin(double lat, double lng) {
        pin = new Location(lat, lng, "");
        int seq = ++geocodeSeq;
        refreshTexts();
        onChange.accept(pin);
        addressText.setTextContent("Finding the address…");
        GoogleMaps.reverseGeocode(lat, lng, (address, status) -> {
            if (seq != geocodeSeq) {
                return; // the map moved again meanwhile
            }
            pin = pin.withAddress(address);
            refreshTexts();
            onChange.accept(pin);
        });
    }

    // ---- search -------------------------------------------------------------------------

    private void scheduleSearch() {
        if (searchTimer >= 0) {
            Window.clearTimeout(searchTimer);
        }
        searchTimer = Window.setTimeout(this::runSearch, SEARCH_DELAY_MILLIS);
    }

    private void runSearch() {
        String q = searchInput.getValue().trim();
        if (!usingGoogle || q.length() < MIN_SEARCH_CHARS) {
            hideResults();
            return;
        }
        Location near = pin != null ? pin : settings.getShopLocation();
        double lat = near != null ? near.getLat() : DEFAULT_LAT;
        double lng = near != null ? near.getLng() : DEFAULT_LNG;
        GoogleMaps.searchAddress(q, lat, lng, (json, status) -> showResults(Json.parseArray(json).objects(), status));
    }

    private void showResults(List<JsonObject> found, String status) {
        Dom.clear(results);
        if (found.isEmpty()) {
            results.appendChild(Dom.text("li", "picker__result picker__result--empty",
                    "ZERO_RESULTS".equals(status) || "OK".equals(status)
                            ? "No match. Try a street, barangay or landmark." : "Search is not available right now."));
        }
        for (JsonObject r : found) {
            HTMLElement li = Dom.el("li", "picker__result");
            li.setAttribute("role", "option");
            li.setAttribute("tabindex", "0");
            Dom.append(li, Dom.icon(Icons.PIN, "picker__result-icon"), Dom.text("span", null, r.getString("address")));
            double lat = r.getDouble("lat", 0);
            double lng = r.getDouble("lng", 0);
            Dom.onClick(li, () -> {
                userMoved = true;
                hideResults();
                searchInput.setValue("");
                searchInput.blur();
                GoogleMaps.flyTo(map, lat, lng, STREET_ZOOM);
            });
            results.appendChild(li);
        }
        results.setHidden(false);
    }

    private void hideResults() {
        if (results != null) {
            results.setHidden(true);
        }
    }

    // ---- GPS & fallback -----------------------------------------------------------------

    private void useGps() {
        Dom.toggleClass(locateButton, "is-loading", true);
        GeoLocator.locate(new GeoLocator.Listener() {
            @Override
            public void onPosition(double lat, double lng) {
                Dom.toggleClass(locateButton, "is-loading", false);
                userMoved = true;
                if (usingGoogle) {
                    GoogleMaps.flyTo(map, lat, lng, STREET_ZOOM);
                } else {
                    pin = new Location(lat, lng, "");
                    refreshTexts();
                    onChange.accept(pin);
                }
            }

            @Override
            public void onError(String message) {
                Dom.toggleClass(locateButton, "is-loading", false);
                if (usingGoogle && pin == null) {
                    addressText.setTextContent("Move the map so the pin is on your pickup point, or search above.");
                } else {
                    Toast.error(message);
                }
            }
        });
    }

    private void initFallback() {
        usingGoogle = false;
        searchBox.setHidden(true);
        Dom.clear(mapBox);
        mapBox.getClassList().remove("map-box--loading");
        mapBox.getClassList().add("picker__map--static");
        HTMLElement art = Dom.div("map-box__art");
        art.setInnerHTML(MapArt.SVG);
        Dom.append(mapBox, art, centerPin, locateButton);
        mapBox.setAttribute("tabindex", "0");
        Dom.onClick(mapBox, this::useGps);
        refreshTexts();
    }

    private void refreshTexts() {
        if (addressText == null) {
            return;
        }
        centerPin.setHidden(!usingGoogle && pin == null);
        Dom.toggleClass(mapBox, "has-pin", pin != null);
        if (pin == null) {
            addressTitle.setTextContent("Pickup point");
            addressText.setTextContent(usingGoogle ? "Move the map so the pin is on your pickup point"
                    : mapBox.getClassList().contains("map-box--loading") ? "Loading the map…"
                    : "Tap the map or the location button to use your current location.");
        } else {
            addressTitle.setTextContent("Pickup & return point");
            addressText.setTextContent(!pin.getAddress().isEmpty() ? pin.getAddress()
                    : usingGoogle ? "Finding the address…" : "Pinned at " + pin.formatCoordinates());
        }
        boolean outside = pin != null && !settings.isWithinDeliveryArea(pin);
        areaWarning.setHidden(!outside);
        Dom.toggleClass(mapBox, "is-outside", outside);
        if (outside) {
            double km = settings.getShopLocation().distanceKmTo(pin);
            double radius = settings.getDeliveryRadiusKm();
            String radiusText = radius == Math.rint(radius) ? Long.toString((long) radius) : Double.toString(radius);
            areaWarning.querySelector("span").setTextContent("Your pin is " + GeoUtils.formatKm(km)
                    + " from the shop. We deliver within " + radiusText
                    + " km, so please choose Walk-in or pin a closer address.");
        }
    }

    @JSBody(params = "e", script = "return e.key || '';")
    private static native String keyOf(Event e);

    private static final String SEARCH_ICON = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" "
            + "stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
            + "<circle cx=\"11\" cy=\"11\" r=\"7\"/><path d=\"M20 20l-3.5-3.5\"/></svg>";
}
