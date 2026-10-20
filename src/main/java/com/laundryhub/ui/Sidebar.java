package com.laundryhub.ui;

import com.laundryhub.util.TextUtils;
import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Admin console sidebar (dark, 230 px): logo, nav with a dot, profile and Log out.
 * On phones/tablets it collapses into a top bar with a menu button.
 */
public class Sidebar extends Component {

    private final List<NavItem> items;
    private final String activeRoute;
    private final String userName;
    private final String userRole;
    private final Runnable onLogOut;
    private HTMLElement root;

    /**
     * @param items       admin pages
     * @param activeRoute route of the current page
     * @param userName    admin name
     * @param userRole    label under the name, e.g. "Owner"
     * @param onLogOut    log out action
     */
    public Sidebar(List<NavItem> items, String activeRoute, String userName, String userRole, Runnable onLogOut) {
        this.items = items;
        this.activeRoute = activeRoute;
        this.userName = userName;
        this.userRole = userRole;
        this.onLogOut = onLogOut;
    }

    @Override
    protected HTMLElement render() {
        root = Dom.el("aside", "sidebar");

        HTMLElement head = Dom.div("sidebar__head");
        HTMLElement brand = Dom.div("sidebar__brand", Dom.icon(Icons.LOGO, "sidebar__logo"),
                Dom.div("sidebar__brand-text",
                        Dom.text("span", "sidebar__name", "Laundry HUB"),
                        Dom.text("span", "sidebar__sub", "Admin console")));
        HTMLElement menu = new IconButton(Icons.MENU, "Open menu", false, this::toggleMenu).getElement();
        menu.getClassList().add("sidebar__menu");
        Dom.append(head, brand, menu);
        root.appendChild(head);

        HTMLElement nav = Dom.el("nav", "sidebar__nav");
        nav.setAttribute("aria-label", "Admin");
        for (NavItem item : items) {
            boolean active = item.getRoute().equals(activeRoute);
            HTMLElement a = Dom.el("a", active ? "sidebar__item is-active" : "sidebar__item");
            a.setAttribute("href", item.getRoute());
            if (active) {
                a.setAttribute("aria-current", "page");
            }
            a.appendChild(Dom.el("span", "sidebar__dot"));
            a.appendChild(Dom.text("span", null, item.getLabel()));
            Dom.onClick(a, () -> Dom.toggleClass(root, "is-open", false));
            nav.appendChild(a);
        }
        root.appendChild(nav);

        HTMLElement logOut = Dom.text("button", "sidebar__logout", "Log out");
        logOut.setAttribute("type", "button");
        if (onLogOut != null) {
            Dom.onClick(logOut, onLogOut);
        }
        HTMLElement profile = Dom.div("sidebar__profile",
                Dom.text("span", "sidebar__avatar", TextUtils.initials(userName)),
                Dom.div("sidebar__who", Dom.text("span", "sidebar__who-name", userName),
                        Dom.text("span", "sidebar__sub", userRole)),
                logOut);
        root.appendChild(profile);
        return root;
    }

    private void toggleMenu() {
        Dom.toggleClass(root, "is-open", !root.getClassList().contains("is-open"));
    }
}
