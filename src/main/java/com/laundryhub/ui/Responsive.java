package com.laundryhub.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * Tracks the screen size with matchMedia and tags {@code <body>} with
 * {@code bp-phone}, {@code bp-tablet} or {@code bp-desktop} (breakpoints in CLAUDE.md).
 */
public final class Responsive {

    /** Screen size classes. */
    public enum Breakpoint {
        /** Below 768 px. */
        PHONE("bp-phone"),
        /** 768–1199 px. */
        TABLET("bp-tablet"),
        /** 1200 px and wider. */
        DESKTOP("bp-desktop");

        private final String cssClass;

        Breakpoint(String cssClass) {
            this.cssClass = cssClass;
        }

        /** @return body class */
        public String getCssClass() {
            return cssClass;
        }
    }

    /** Smallest tablet width in px. */
    public static final int TABLET_MIN = 768;
    /** Smallest desktop width in px. */
    public static final int DESKTOP_MIN = 1200;

    /** The browser's MediaQueryList. */
    interface MediaQueryList extends EventTarget {
        @JSProperty
        boolean isMatches();
    }

    private static final List<Consumer<Breakpoint>> LISTENERS = new ArrayList<>();
    private static MediaQueryList tabletQuery;
    private static MediaQueryList desktopQuery;
    private static Breakpoint current;

    private Responsive() {
    }

    /** Starts listening; call once at startup. */
    public static void init() {
        tabletQuery = matchMedia("(min-width: " + TABLET_MIN + "px)");
        desktopQuery = matchMedia("(min-width: " + DESKTOP_MIN + "px)");
        tabletQuery.addEventListener("change", e -> update());
        desktopQuery.addEventListener("change", e -> update());
        update();
    }

    /** @return the current breakpoint */
    public static Breakpoint current() {
        if (current == null) {
            init();
        }
        return current;
    }

    /** @return true at desktop width */
    public static boolean isDesktop() {
        return current() == Breakpoint.DESKTOP;
    }

    /** @return true at phone width */
    public static boolean isPhone() {
        return current() == Breakpoint.PHONE;
    }

    /**
     * @param listener called when the breakpoint changes
     * @return a handle that removes the listener when run
     */
    public static Runnable addListener(Consumer<Breakpoint> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    private static void update() {
        Breakpoint next = desktopQuery.isMatches() ? Breakpoint.DESKTOP
                : tabletQuery.isMatches() ? Breakpoint.TABLET : Breakpoint.PHONE;
        if (next == current) {
            return;
        }
        current = next;
        var body = HTMLDocument.current().getBody();
        for (Breakpoint b : Breakpoint.values()) {
            Dom.toggleClass(body, b.getCssClass(), b == next);
        }
        for (Consumer<Breakpoint> l : new ArrayList<>(LISTENERS)) {
            l.accept(next);
        }
    }

    @JSBody(params = "query", script = "return window.matchMedia(query);")
    private static native MediaQueryList matchMedia(String query);
}
