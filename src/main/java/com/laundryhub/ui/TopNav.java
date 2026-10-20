package com.laundryhub.ui;

import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Desktop top navigation: logo, links, user name and sign out. Hidden on phones.
 */
public class TopNav extends Component {

    private final List<NavItem> items;
    private final String activeRoute;
    private final String userName;
    private final Runnable onSignOut;

    /**
     * @param items       links
     * @param activeRoute route of the current page
     * @param userName    signed-in user's name, may be null
     * @param onSignOut   sign out action, may be null
     */
    public TopNav(List<NavItem> items, String activeRoute, String userName, Runnable onSignOut) {
        this.items = items;
        this.activeRoute = activeRoute;
        this.userName = userName;
        this.onSignOut = onSignOut;
    }

    /** @return the logo tile + "Laundry HUB" wordmark */
    public static HTMLElement brand() {
        return Dom.div("brand", Dom.icon(Icons.LOGO, "brand__tile"), Dom.text("span", "brand__name", "Laundry HUB"));
    }

    @Override
    protected HTMLElement render() {
        HTMLElement header = Dom.el("header", "top-nav");
        HTMLElement inner = Dom.div("top-nav__inner");
        HTMLElement home = Dom.el("a", "top-nav__brand");
        home.setAttribute("href", items.isEmpty() ? "#/" : items.get(0).getRoute());
        home.appendChild(brand());
        inner.appendChild(home);

        HTMLElement links = Dom.el("nav", "top-nav__links");
        links.setAttribute("aria-label", "Main");
        for (NavItem item : items) {
            boolean active = item.getRoute().equals(activeRoute);
            HTMLElement a = Dom.text("a", active ? "top-nav__link is-active" : "top-nav__link", item.getLabel());
            a.setAttribute("href", item.getRoute());
            if (active) {
                a.setAttribute("aria-current", "page");
            }
            links.appendChild(a);
        }
        inner.appendChild(links);

        HTMLElement right = Dom.div("top-nav__user");
        if (userName != null) {
            right.appendChild(Dom.text("span", "top-nav__name", userName));
        }
        if (onSignOut != null) {
            right.appendChild(new Button("Sign out", Button.Variant.OUTLINE, onSignOut).getElement());
        }
        inner.appendChild(right);
        header.appendChild(inner);
        return header;
    }
}
