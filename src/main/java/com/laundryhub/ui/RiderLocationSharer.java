package com.laundryhub.ui;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Location;
import com.laundryhub.repository.RiderJobRepository;
import com.laundryhub.util.Callback;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.jso.browser.Window;

/**
 * While a rider is on duty and a rider page is open: follows the GPS (watchPosition) and sends the
 * latest position to riders.last_lat / last_lng / last_seen every 10 seconds, so customers see the
 * rider move on the tracking map.
 */
public final class RiderLocationSharer {

    /** How often the position is sent. */
    public static final int SEND_EVERY_MILLIS = 10000;

    private static final List<Consumer<Location>> LISTENERS = new ArrayList<>();
    private static RiderJobRepository repository;
    private static Location latest;
    private static int watchId = -1;
    private static int timer = -1;
    private static boolean errorShown;

    private RiderLocationSharer() {
    }

    /** @return the last known GPS position, or null */
    public static Location getLatest() {
        return latest;
    }

    /** @return true while sharing */
    public static boolean isRunning() {
        return timer >= 0;
    }

    /**
     * @param listener called on every new position
     * @return a handle that removes the listener when run
     */
    public static Runnable addListener(Consumer<Location> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Starts following and sending (no-op when already running).
     *
     * @param repo rider repository used to send the position
     */
    public static void start(RiderJobRepository repo) {
        repository = repo;
        if (isRunning()) {
            return;
        }
        errorShown = false;
        watchId = GeoLocator.watch(new GeoLocator.Listener() {
            @Override
            public void onPosition(double lat, double lng) {
                latest = new Location(lat, lng, "");
                for (Consumer<Location> l : new ArrayList<>(LISTENERS)) {
                    l.accept(latest);
                }
            }

            @Override
            public void onError(String message) {
                if (!errorShown) {
                    errorShown = true;
                    Toast.error("Location sharing is off: " + message);
                }
            }
        });
        timer = Window.setInterval(RiderLocationSharer::send, SEND_EVERY_MILLIS);
    }

    /** Stops following and sending. */
    public static void stop() {
        GeoLocator.stopWatching(watchId);
        watchId = -1;
        if (timer >= 0) {
            Window.clearInterval(timer);
            timer = -1;
        }
    }

    private static void send() {
        if (latest == null || repository == null) {
            return;
        }
        repository.updateLocation(latest, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Sent; last_seen is refreshed even when the rider is standing still.
            }

            @Override
            public void onError(ApiException error) {
                // Try again on the next tick.
            }
        });
    }
}
