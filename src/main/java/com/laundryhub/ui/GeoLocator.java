package com.laundryhub.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * The browser's GPS (navigator.geolocation). Works only on https or localhost/127.0.0.1.
 */
public final class GeoLocator {

    /** Receives a position. */
    public interface Listener {
        /**
         * @param lat latitude
         * @param lng longitude
         */
        void onPosition(double lat, double lng);

        /**
         * @param message friendly explanation
         */
        void onError(String message);
    }

    @JSFunctor
    interface PositionCallback extends JSObject {
        void call(double lat, double lng);
    }

    @JSFunctor
    interface ErrorCallback extends JSObject {
        void call(int code);
    }

    private static final int UNSUPPORTED = 0;
    private static final int PERMISSION_DENIED = 1;
    private static final int UNAVAILABLE = 2;
    private static final int TIMEOUT = 3;
    private static final int INSECURE = 4;

    private GeoLocator() {
    }

    /**
     * Asks for the current position once (high accuracy, 15 s timeout).
     *
     * @param listener receives the position or a friendly error
     */
    public static void locate(Listener listener) {
        getCurrentPosition(listener::onPosition, code -> listener.onError(message(code)));
    }

    /**
     * Follows the position continuously (navigator.geolocation.watchPosition).
     *
     * @param listener receives every new position and errors
     * @return watch id for {@link #stopWatching(int)}, or -1 when not available
     */
    public static int watch(Listener listener) {
        return watchPosition(listener::onPosition, code -> listener.onError(message(code)));
    }

    /**
     * @param watchId id from {@link #watch(Listener)}
     */
    public static void stopWatching(int watchId) {
        if (watchId >= 0) {
            clearWatch(watchId);
        }
    }

    /**
     * @param code error code
     * @return text for the user
     */
    static String message(int code) {
        switch (code) {
            case PERMISSION_DENIED:
                return "Location permission is blocked. Allow location for this site in your browser settings, "
                        + "or tap the map to pin your location.";
            case UNAVAILABLE:
                return "We couldn't find your location. Check that GPS/location is on, or tap the map instead.";
            case TIMEOUT:
                return "Finding your location took too long. Please try again.";
            case INSECURE:
                return "Location only works on a secure (https) page. Open the app from its https address.";
            default:
                return "Your browser does not support location. Please tap the map to pin your location.";
        }
    }

    @JSBody(params = {"ok", "fail"}, script = ""
            + "if (!window.isSecureContext) { fail(" + INSECURE + "); return; }"
            + "if (!navigator.geolocation) { fail(" + UNSUPPORTED + "); return; }"
            + "navigator.geolocation.getCurrentPosition("
            + "  function (p) { ok(p.coords.latitude, p.coords.longitude); },"
            + "  function (e) { fail(e.code); },"
            + "  { enableHighAccuracy: true, timeout: 15000, maximumAge: 30000 });")
    private static native void getCurrentPosition(PositionCallback ok, ErrorCallback fail);

    @JSBody(params = {"ok", "fail"}, script = ""
            + "if (!window.isSecureContext) { fail(" + INSECURE + "); return -1; }"
            + "if (!navigator.geolocation) { fail(" + UNSUPPORTED + "); return -1; }"
            + "return navigator.geolocation.watchPosition("
            + "  function (p) { ok(p.coords.latitude, p.coords.longitude); },"
            + "  function (e) { fail(e.code); },"
            + "  { enableHighAccuracy: true, timeout: 20000, maximumAge: 5000 });")
    private static native int watchPosition(PositionCallback ok, ErrorCallback fail);

    @JSBody(params = "id", script = "if (navigator.geolocation) navigator.geolocation.clearWatch(id);")
    private static native void clearWatch(int id);
}
