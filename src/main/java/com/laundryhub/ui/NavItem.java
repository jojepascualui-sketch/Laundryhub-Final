package com.laundryhub.ui;

/**
 * One entry of a navigation bar (bottom nav, top nav or admin sidebar).
 */
public final class NavItem {

    private final String label;
    private final String route;
    private final String iconSvg;

    /**
     * @param label   text
     * @param route   hash route, e.g. "#/home"
     * @param iconSvg icon from {@link Icons}, may be null
     */
    public NavItem(String label, String route, String iconSvg) {
        this.label = label;
        this.route = route;
        this.iconSvg = iconSvg;
    }

    /** @return text */
    public String getLabel() {
        return label;
    }

    /** @return hash route */
    public String getRoute() {
        return route;
    }

    /** @return icon, may be null */
    public String getIconSvg() {
        return iconSvg;
    }
}
