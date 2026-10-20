package com.laundryhub.ui;

import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Phone bottom navigation (Home · Orders · Profile). Hidden on desktop, where {@link TopNav} is used.
 */
public class BottomNav extends Component {

    private final List<NavItem> items;
    private final String activeRoute;

    /**
     * @param items       entries
     * @param activeRoute route of the current page
     */
    public BottomNav(List<NavItem> items, String activeRoute) {
        this.items = items;
        this.activeRoute = activeRoute;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement nav = Dom.el("nav", "bottom-nav");
        nav.setAttribute("aria-label", "Main");
        for (NavItem item : items) {
            boolean active = item.getRoute().equals(activeRoute);
            HTMLElement a = Dom.el("a", active ? "bottom-nav__item is-active" : "bottom-nav__item");
            a.setAttribute("href", item.getRoute());
            if (active) {
                a.setAttribute("aria-current", "page");
            }
            if (item.getIconSvg() != null) {
                a.appendChild(Dom.icon(item.getIconSvg(), null));
            }
            a.appendChild(Dom.text("span", null, item.getLabel()));
            nav.appendChild(a);
        }
        return nav;
    }
}
