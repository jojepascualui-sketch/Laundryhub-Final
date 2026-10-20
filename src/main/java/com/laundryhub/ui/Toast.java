package com.laundryhub.ui;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Short message at the bottom of the screen that disappears by itself.
 */
public final class Toast {

    /** Message kind. */
    public enum Kind { INFO, SUCCESS, ERROR }

    private static final int SHOW_MILLIS = 3500;
    private static final int ERROR_SHOW_MILLIS = 5000;
    private static HTMLElement container;

    private Toast() {
    }

    /** @param message neutral message */
    public static void info(String message) {
        show(message, Kind.INFO);
    }

    /** @param message success message */
    public static void success(String message) {
        show(message, Kind.SUCCESS);
    }

    /** @param message error message */
    public static void error(String message) {
        show(message, Kind.ERROR);
    }

    /**
     * @param message text
     * @param kind    color
     */
    public static void show(String message, Kind kind) {
        if (container == null || container.getParentNode() == null) {
            container = Dom.el("div", "toast-container");
            container.setAttribute("aria-live", "polite");
            HTMLDocument.current().getBody().appendChild(container);
        }
        HTMLElement toast = Dom.text("div", "toast toast--" + kind.name().toLowerCase(), message);
        toast.setAttribute("role", kind == Kind.ERROR ? "alert" : "status");
        container.appendChild(toast);
        Window.setTimeout(() -> Dom.remove(toast), kind == Kind.ERROR ? ERROR_SHOW_MILLIS : SHOW_MILLIS);
    }
}
