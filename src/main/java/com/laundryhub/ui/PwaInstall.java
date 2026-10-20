package com.laundryhub.ui;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Storage;

/**
 * Makes the site installable: registers sw.js, keeps the browser's "beforeinstallprompt" event
 * so an "Install app" button can show it later, and detects iPhone/iPad Safari (which has no
 * install prompt; users tap Share → Add to Home Screen).
 */
public final class PwaInstall {

    @JSFunctor
    interface Signal extends JSObject {
        void fire();
    }

    @JSFunctor
    interface ChoiceHandler extends JSObject {
        void handle(boolean accepted);
    }

    private static final String DISMISSED_KEY = "laundryhub.installDismissed";
    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static boolean promptReady;

    private PwaInstall() {
    }

    /** Call once at startup: registers the service worker and listens for the install prompt. */
    public static void init() {
        registerServiceWorker();
        listen(() -> {
            promptReady = true;
            notifyListeners();
        }, () -> {
            promptReady = false;
            notifyListeners();
        });
    }

    /** @return true when the browser offered installation ("Install app" can be shown) */
    public static boolean canPrompt() {
        return promptReady && !isStandalone();
    }

    /** @return true on iPhone/iPad Safari when not installed yet (show "Add to Home Screen" help) */
    public static boolean needsIosInstructions() {
        return isIos() && !isStandalone();
    }

    /** @return true when the user closed the install card on this device */
    public static boolean isDismissed() {
        try {
            Storage s = Storage.getLocalStorage();
            return s != null && "1".equals(s.getItem(DISMISSED_KEY));
        } catch (RuntimeException blocked) {
            return false;
        }
    }

    /** Remembers that the user does not want the install card. */
    public static void dismiss() {
        try {
            Storage s = Storage.getLocalStorage();
            if (s != null) {
                s.setItem(DISMISSED_KEY, "1");
            }
        } catch (RuntimeException blocked) {
            // not remembered; the card shows again next time
        }
    }

    /**
     * @param listener called when installing becomes possible or was done
     * @return a handle that removes the listener when run
     */
    public static Runnable addListener(Runnable listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Shows the browser's install dialog.
     *
     * @param onAccepted called when the user installed the app
     */
    public static void prompt(Runnable onAccepted) {
        if (!promptReady) {
            return;
        }
        showPrompt(accepted -> {
            promptReady = false;
            notifyListeners();
            if (accepted) {
                onAccepted.run();
            }
        });
    }

    private static void notifyListeners() {
        for (Runnable l : new ArrayList<>(LISTENERS)) {
            l.run();
        }
    }

    @JSBody(script = "if ('serviceWorker' in navigator && window.isSecureContext) {"
            + " navigator.serviceWorker.register('sw.js').catch(function (e) { console.warn('Service worker:', e); }); }")
    private static native void registerServiceWorker();

    @JSBody(params = {"onReady", "onInstalled"}, script = ""
            + "window.addEventListener('beforeinstallprompt', function (e) {"
            + "  e.preventDefault(); window.lhInstallEvent = e; onReady(); });"
            + "window.addEventListener('appinstalled', function () { window.lhInstallEvent = null; onInstalled(); });")
    private static native void listen(Signal onReady, Signal onInstalled);

    @JSBody(params = "done", script = ""
            + "var lhEvent = window.lhInstallEvent; if (!lhEvent) { done(false); return; }"
            + "window.lhInstallEvent = null; lhEvent.prompt();"
            + "lhEvent.userChoice.then(function (c) { done(c && c.outcome === 'accepted'); }, function () { done(false); });")
    private static native void showPrompt(ChoiceHandler done);

    @JSBody(script = "return !!(window.matchMedia && window.matchMedia('(display-mode: standalone)').matches)"
            + " || window.navigator.standalone === true;")
    private static native boolean isStandalone();

    @JSBody(script = "var lhUa = navigator.userAgent || '';"
            + "return /iPhone|iPad|iPod/.test(lhUa) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);")
    private static native boolean isIos();
}
