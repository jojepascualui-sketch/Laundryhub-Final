package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Pill button in the design's styles.
 */
public class Button extends Component {

    /** Visual style. */
    public enum Variant {
        /** Accent blue, white text (Continue, Sign in, Accept booking). */
        PRIMARY("btn--primary"),
        /** White with grey border (Decline, Open, Continue with Google). */
        OUTLINE("btn--outline"),
        /** Ink background (Back to home, Navigate in Google Maps). */
        DARK("btn--dark"),
        /** White pill on blue (Start laundry order). */
        LIGHT("btn--light"),
        /** Text only in accent (Sign up, View all). */
        LINK("btn--link");

        private final String cssClass;

        Variant(String cssClass) {
            this.cssClass = cssClass;
        }

        String getCssClass() {
            return cssClass;
        }
    }

    // OOP: Encapsulation — state changes go through setters that keep the DOM in sync.
    private final Variant variant;
    private String label;
    private final String iconSvg;
    private final boolean iconAfter;
    private Runnable onClick;
    private boolean disabled;
    private boolean loading;
    private boolean block;

    /**
     * @param label   button text
     * @param variant style
     * @param onClick action, may be null
     */
    public Button(String label, Variant variant, Runnable onClick) {
        this(label, variant, null, false, onClick);
    }

    /**
     * @param label     button text
     * @param variant   style
     * @param iconSvg   icon from {@link Icons}, may be null
     * @param iconAfter true to put the icon after the text
     * @param onClick   action, may be null
     */
    public Button(String label, Variant variant, String iconSvg, boolean iconAfter, Runnable onClick) {
        this.label = label;
        this.variant = variant;
        this.iconSvg = iconSvg;
        this.iconAfter = iconAfter;
        this.onClick = onClick;
    }

    /**
     * @param fullWidth stretch to the container width
     * @return this button
     */
    public Button block(boolean fullWidth) {
        this.block = fullWidth;
        updateClasses();
        return this;
    }

    /** @param action new click action */
    public void setOnClick(Runnable action) {
        this.onClick = action;
    }

    /** @param value true to disable (grey, not clickable) */
    public void setDisabled(boolean value) {
        this.disabled = value;
        updateState();
    }

    /** @return true when disabled */
    public boolean isDisabled() {
        return disabled;
    }

    /** @param value true to show a spinner and block clicks while waiting for the server */
    public void setLoading(boolean value) {
        this.loading = value;
        updateState();
    }

    /** @param text new label */
    public void setLabel(String text) {
        this.label = text;
        HTMLElement span = getElement().querySelector(".btn__label");
        if (span != null) {
            span.setTextContent(text);
        }
    }

    @Override
    protected HTMLElement render() {
        HTMLElement b = Dom.el("button", null);
        b.setAttribute("type", "button");
        if (iconSvg != null && !iconAfter) {
            b.appendChild(Dom.icon(iconSvg, null));
        }
        b.appendChild(Dom.text("span", "btn__label", label));
        if (iconSvg != null && iconAfter) {
            b.appendChild(Dom.icon(iconSvg, null));
        }
        b.appendChild(Dom.el("span", "spinner spinner--small btn__spinner"));
        b.addEventListener("click", e -> {
            if (!disabled && !loading && onClick != null) {
                onClick.run();
            }
        });
        updateClasses(b);
        updateState(b);
        return b;
    }

    private void updateClasses() {
        updateClasses(getElement());
    }

    private void updateClasses(HTMLElement b) {
        b.setClassName("btn " + variant.getCssClass() + (block ? " btn--block" : ""));
        updateState(b);
    }

    private void updateState() {
        updateState(getElement());
    }

    private void updateState(HTMLElement b) {
        if (disabled || loading) {
            b.setAttribute("disabled", "");
        } else {
            b.removeAttribute("disabled");
        }
        b.setAttribute("aria-busy", loading ? "true" : "false");
        Dom.toggleClass(b, "is-loading", loading);
    }
}
