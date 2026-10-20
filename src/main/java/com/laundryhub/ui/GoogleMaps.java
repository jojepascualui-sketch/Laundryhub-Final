package com.laundryhub.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Java bindings (TeaVM JSO) for the parts of the Google Maps JavaScript API the app uses:
 * Map, Marker, Circle, Polyline, LatLng, LatLngBounds, events and the Geocoder.
 * The API script itself is loaded by index.html with the key from config.js.
 *
 * <p>Note: TeaVM renames @JSBody parameters to short names (a, b, c…), so local variables inside
 * the scripts use an "lh" prefix to never collide with them.
 */
public final class GoogleMaps {

    /** google.maps.LatLng */
    public interface LatLng extends JSObject {
        /** @return latitude */
        double lat();

        /** @return longitude */
        double lng();
    }

    /** google.maps.Map */
    public interface GMap extends JSObject {
        /** @param position new center (animated) */
        void panTo(LatLng position);

        /** @param zoom zoom level */
        void setZoom(int zoom);

        /** @return current zoom */
        int getZoom();
    }

    /** google.maps.Marker */
    public interface Marker extends JSObject {
        /** @param position new position */
        void setPosition(LatLng position);

        /** @return position */
        LatLng getPosition();

        /** @param visible show or hide */
        void setVisible(boolean visible);

        /** @param map map, or null to remove */
        void setMap(GMap map);
    }

    /** google.maps.Polyline or Circle (anything that can be removed from the map) */
    public interface Overlay extends JSObject {
        /** @param map map, or null to remove */
        void setMap(GMap map);
    }

    /** A registered map event; call {@link #remove()} to stop listening. */
    public interface Listener extends JSObject {
        /** Stops listening. */
        void remove();
    }

    /** Receives a position from a map event. */
    @JSFunctor
    public interface PositionHandler extends JSObject {
        /**
         * @param position where the user tapped or dropped the pin
         */
        void handle(LatLng position);
    }

    /** Receives a reverse-geocoding result. */
    @JSFunctor
    public interface AddressHandler extends JSObject {
        /**
         * @param address formatted address ("" when none)
         * @param status  Geocoder status, e.g. OK, ZERO_RESULTS, REQUEST_DENIED
         */
        void handle(String address, String status);
    }

    /** Marker look. */
    public enum MarkerStyle {
        /** Orange customer pin (design color #E0662A). */
        PIN,
        /** Dark round shop marker. */
        SHOP,
        /** Accent round rider marker. */
        RIDER
    }

    private static final int POLL_MILLIS = 150;
    private static final int MAX_WAIT_MILLIS = 12000;
    private static final int AUTH_POLL_MILLIS = 400;

    private GoogleMaps() {
    }

    /** @return true when the Maps API has finished loading */
    @JSBody(script = "return !!(window.google && google.maps && google.maps.Map);")
    public static native boolean isLoaded();

    /** @return true when config.js has a Maps key (otherwise the script is never requested) */
    @JSBody(script = "return !!(window.LH_CONFIG && window.LH_CONFIG.GOOGLE_MAPS_KEY);")
    public static native boolean hasKey();

    /**
     * Waits for the API (loaded asynchronously by index.html).
     *
     * @param onReady    called when ready
     * @param onFailure  called when there is no key or it did not load in time (e.g. key not allowed)
     */
    public static void whenReady(Runnable onReady, Runnable onFailure) {
        if (isLoaded()) {
            onReady.run();
            return;
        }
        if (!hasKey()) {
            onFailure.run();
            return;
        }
        int[] waited = {0};
        int[] timer = new int[1];
        timer[0] = Window.setInterval(() -> {
            waited[0] += POLL_MILLIS;
            if (isLoaded()) {
                Window.clearInterval(timer[0]);
                onReady.run();
            } else if (waited[0] >= MAX_WAIT_MILLIS || authFailed()) {
                Window.clearInterval(timer[0]);
                onFailure.run();
            }
        }, POLL_MILLIS);
    }

    /**
     * Google can reject the key after the script loaded (wrong key, or this website is not on the
     * key's allowed list); it then draws an error box inside the map. This watches for that for a
     * short time after a map is created and calls {@code onFailure} so the page can show its fallback.
     *
     * @param onFailure called once when Google reports a key problem
     */
    public static void watchAuthFailure(Runnable onFailure) {
        int[] waited = {0};
        int[] timer = new int[1];
        timer[0] = Window.setInterval(() -> {
            waited[0] += AUTH_POLL_MILLIS;
            if (authFailed()) {
                Window.clearInterval(timer[0]);
                onFailure.run();
            } else if (waited[0] >= MAX_WAIT_MILLIS) {
                Window.clearInterval(timer[0]);
            }
        }, AUTH_POLL_MILLIS);
    }

    /** @return true when Google reported a key problem (gm_authFailure) */
    @JSBody(script = "return !!window.LH_MAPS_AUTH_FAILED;")
    private static native boolean authFailed();

    /**
     * @param lat latitude
     * @param lng longitude
     * @return a LatLng
     */
    @JSBody(params = {"lat", "lng"}, script = "return new google.maps.LatLng(lat, lng);")
    public static native LatLng latLng(double lat, double lng);

    /**
     * Creates a map with only the zoom buttons (clean look like the design).
     *
     * @param element container (needs a height)
     * @param lat     center latitude
     * @param lng     center longitude
     * @param zoom    zoom level
     * @return the map
     */
    @JSBody(params = {"element", "lat", "lng", "zoom"}, script = ""
            + "return new google.maps.Map(element, { center: { lat: lat, lng: lng }, zoom: zoom,"
            + " disableDefaultUI: true, zoomControl: true, clickableIcons: false, gestureHandling: 'greedy' });")
    public static native GMap createMap(HTMLElement element, double lat, double lng, int zoom);

    /**
     * @param map       map
     * @param lat       latitude
     * @param lng       longitude
     * @param style     look
     * @param draggable true when the user may drag it
     * @param title     tooltip / accessible name
     * @return the marker
     */
    public static Marker createMarker(GMap map, double lat, double lng, MarkerStyle style, boolean draggable, String title) {
        return createMarkerJs(map, lat, lng, iconSvg(style), style == MarkerStyle.PIN, draggable, title);
    }

    @JSBody(params = {"map", "lat", "lng", "svg", "isPin", "draggable", "title"}, script = ""
            + "var lhSize = isPin ? new google.maps.Size(34, 44) : new google.maps.Size(36, 36);"
            + "var lhAnchor = isPin ? new google.maps.Point(17, 43) : new google.maps.Point(18, 18);"
            + "return new google.maps.Marker({ map: map, position: { lat: lat, lng: lng }, draggable: draggable,"
            + " title: title, icon: { url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg),"
            + " scaledSize: lhSize, anchor: lhAnchor } });")
    private static native Marker createMarkerJs(GMap map, double lat, double lng, String svg, boolean isPin,
                                                boolean draggable, String title);

    /**
     * Faint circle showing the delivery area.
     *
     * @param map      map
     * @param lat      center latitude
     * @param lng      center longitude
     * @param radiusKm radius in km
     * @return the circle
     */
    @JSBody(params = {"map", "lat", "lng", "radiusKm"}, script = ""
            + "return new google.maps.Circle({ map: map, center: { lat: lat, lng: lng }, radius: radiusKm * 1000,"
            + " strokeColor: '#1E63A8', strokeOpacity: 0.35, strokeWeight: 1, fillColor: '#1E63A8', fillOpacity: 0.05,"
            + " clickable: false });")
    public static native Overlay createAreaCircle(GMap map, double lat, double lng, double radiusKm);

    /**
     * Dashed accent route line (tracking screen).
     *
     * @param map  map
     * @param lats latitudes
     * @param lngs longitudes
     * @return the line
     */
    @JSBody(params = {"map", "lats", "lngs"}, script = ""
            + "var lhPath = []; for (var lhI = 0; lhI < lats.length; lhI++) lhPath.push({ lat: lats[lhI], lng: lngs[lhI] });"
            + "return new google.maps.Polyline({ map: map, path: lhPath, strokeOpacity: 0,"
            + " icons: [{ icon: { path: 'M 0,-1 0,1', strokeOpacity: 1, strokeColor: '#1E63A8', scale: 3 },"
            + " offset: '0', repeat: '14px' }] });")
    public static native Overlay createDashedLine(GMap map, double[] lats, double[] lngs);

    /**
     * Zooms the map so all points are visible.
     *
     * @param map     map
     * @param lats    latitudes
     * @param lngs    longitudes
     * @param padding padding in px
     */
    @JSBody(params = {"map", "lats", "lngs", "padding"}, script = ""
            + "var lhBounds = new google.maps.LatLngBounds();"
            + "for (var lhI = 0; lhI < lats.length; lhI++) lhBounds.extend({ lat: lats[lhI], lng: lngs[lhI] });"
            + "map.fitBounds(lhBounds, padding);")
    public static native void fitPoints(GMap map, double[] lats, double[] lngs, int padding);

    /**
     * @param map     map
     * @param handler receives the tapped position
     * @return the listener
     */
    @JSBody(params = {"map", "handler"}, script = ""
            + "return map.addListener('click', function (e) { if (e.latLng) handler(e.latLng); });")
    public static native Listener onMapClick(GMap map, PositionHandler handler);

    /**
     * @param marker  marker
     * @param handler receives the new position after a drag
     * @return the listener
     */
    @JSBody(params = {"marker", "handler"}, script = ""
            + "return marker.addListener('dragend', function (e) { handler(e.latLng); });")
    public static native Listener onMarkerDragEnd(Marker marker, PositionHandler handler);

    /**
     * Reverse geocoding: position → formatted address.
     *
     * @param lat     latitude
     * @param lng     longitude
     * @param handler receives the address and the status
     */
    @JSBody(params = {"lat", "lng", "handler"}, script = ""
            + "new google.maps.Geocoder().geocode({ location: { lat: lat, lng: lng } }, function (results, status) {"
            + "  handler(status === 'OK' && results && results[0] ? results[0].formatted_address : '', String(status));"
            + "});")
    public static native void reverseGeocode(double lat, double lng, AddressHandler handler);

    /**
     * @param style marker look
     * @return SVG markup for the marker icon
     */
    static String iconSvg(MarkerStyle style) {
        switch (style) {
            case SHOP:
                return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"36\" height=\"36\" viewBox=\"0 0 36 36\">"
                        + "<circle cx=\"18\" cy=\"18\" r=\"16\" fill=\"#14212B\" stroke=\"#fff\" stroke-width=\"3\"/>"
                        + "<path d=\"M12 24V15l6-4.5 6 4.5v9M15.5 24v-4.5h5V24\" fill=\"none\" stroke=\"#fff\" stroke-width=\"1.8\""
                        + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";
            case RIDER:
                return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"36\" height=\"36\" viewBox=\"0 0 36 36\">"
                        + "<circle cx=\"18\" cy=\"18\" r=\"16\" fill=\"#1E63A8\" stroke=\"#fff\" stroke-width=\"3\"/>"
                        + "<path d=\"M9 13h10v8H9zM19 16h3.5l2.5 2.5V21h-6\" fill=\"none\" stroke=\"#fff\" stroke-width=\"1.8\""
                        + " stroke-linejoin=\"round\"/><circle cx=\"12.5\" cy=\"22.5\" r=\"1.6\" fill=\"#fff\"/>"
                        + "<circle cx=\"22\" cy=\"22.5\" r=\"1.6\" fill=\"#fff\"/></svg>";
            default:
                return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"34\" height=\"44\" viewBox=\"0 0 34 44\">"
                        + "<path d=\"M17 43s15-14.2 15-26A15 15 0 0 0 2 17c0 11.8 15 26 15 26z\" fill=\"#E0662A\""
                        + " stroke=\"#fff\" stroke-width=\"2\"/><circle cx=\"17\" cy=\"17\" r=\"6\" fill=\"#fff\"/></svg>";
        }
    }
}
