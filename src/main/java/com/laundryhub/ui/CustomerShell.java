package com.laundryhub.ui;

import java.util.Arrays;
import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Layout of the customer pages: top nav on desktop, bottom nav (Home · Orders · Profile) on
 * phones/tablets, content in between.
 */
public class CustomerShell extends Component {

    /** Customer navigation entries. */
    public static final List<NavItem> NAV = Arrays.asList(
            new NavItem("Home", "#/home", Icons.HOME),
            new NavItem("Orders", "#/orders", Icons.ORDERS),
            new NavItem("Profile", "#/profile", Icons.PROFILE));

    private final String activeRoute;
    private final String userName;
    private final Runnable onSignOut;
    private final HTMLElement content;
    private final boolean showBottomNav;

    /**
     * @param activeRoute   route of the current page (highlighted in the navs)
     * @param userName      signed-in name for the desktop nav
     * @param onSignOut     sign out action
     * @param content       page content
     * @param showBottomNav false for full-screen flows (wizard) that have their own bottom bar
     */
    public CustomerShell(String activeRoute, String userName, Runnable onSignOut, HTMLElement content,
                         boolean showBottomNav) {
        this.activeRoute = activeRoute;
        this.userName = userName;
        this.onSignOut = onSignOut;
        this.content = content;
        this.showBottomNav = showBottomNav;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement shell = Dom.div(showBottomNav ? "shell shell--tabs" : "shell");
        shell.appendChild(new TopNav(NAV, activeRoute, userName, onSignOut).getElement());
        content.getClassList().add("shell__main");
        shell.appendChild(content);
        if (showBottomNav) {
            shell.appendChild(new BottomNav(NAV, activeRoute).getElement());
        }
        return shell;
    }
}
