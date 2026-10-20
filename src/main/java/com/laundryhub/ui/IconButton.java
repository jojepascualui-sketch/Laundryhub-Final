package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Round 44 px white button with an icon (back, bell, call).
 */
public class IconButton extends Component {

    private final String iconSvg;
    private final String ariaLabel;
    private final boolean accent;
    private final Runnable onClick;

    /**
     * @param iconSvg   icon from {@link Icons}
     * @param ariaLabel text for screen readers, e.g. "Back"
     * @param accent    true for the filled accent style (call button)
     * @param onClick   action
     */
    public IconButton(String iconSvg, String ariaLabel, boolean accent, Runnable onClick) {
        this.iconSvg = iconSvg;
        this.ariaLabel = ariaLabel;
        this.accent = accent;
        this.onClick = onClick;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement b = Dom.el("button", accent ? "icon-btn icon-btn--accent" : "icon-btn");
        b.setAttribute("type", "button");
        b.setAttribute("aria-label", ariaLabel);
        b.appendChild(Dom.icon(iconSvg, null));
        if (onClick != null) {
            Dom.onClick(b, onClick);
        }
        return b;
    }
}
