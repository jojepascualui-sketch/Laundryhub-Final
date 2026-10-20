package com.laundryhub.ui;

import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Hash router: {@code #/order/service}, {@code #/orders/:id}, {@code #/admin/orders}…
 * Checks each page's allowed roles before showing it.
 */
public final class Router {

    /** Creates a page for a route; receives the route parameters (e.g. "id"). */
    public interface PageFactory {
        /**
         * @param params values of ":name" parts in the route pattern
         * @return the page
         */
        Page create(Map<String, String> params);
    }

    private static final String APP_NAME = "Laundry HUB";
    private static final String SIGN_IN = "#/signin";

    private final HTMLElement outlet;
    private final Supplier<User> currentUser;
    private final List<String[]> patterns = new ArrayList<>();
    private final List<PageFactory> factories = new ArrayList<>();
    private String fallbackRoute = SIGN_IN;
    private Page current;

    /**
     * @param outlet      element that holds the current page
     * @param currentUser returns the signed-in user, or null
     */
    public Router(HTMLElement outlet, Supplier<User> currentUser) {
        this.outlet = outlet;
        this.currentUser = currentUser;
    }

    /**
     * @param pattern route like "/orders/:id"
     * @param factory creates the page
     * @return this router
     */
    public Router add(String pattern, PageFactory factory) {
        patterns.add(split(pattern));
        factories.add(factory);
        return this;
    }

    /**
     * @param route where unknown routes go, e.g. "#/signin"
     * @return this router
     */
    public Router fallback(String route) {
        this.fallbackRoute = route;
        return this;
    }

    /** Starts listening to hash changes and shows the current route. */
    public void start() {
        Window.current().addEventListener("hashchange", e -> show());
        show();
    }

    /**
     * @param route e.g. "#/home"
     */
    public void navigate(String route) {
        String target = route.startsWith("#") ? route : "#" + route;
        if (target.equals(Window.current().getLocation().getHash())) {
            show();
        } else {
            Window.current().getLocation().setHash(target);
        }
    }

    /** @return the current hash path without "#", e.g. "/home" */
    public static String currentPath() {
        String hash = Window.current().getLocation().getHash();
        String path = hash.startsWith("#") ? hash.substring(1) : hash;
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    /** Re-renders the current route (e.g. after sign in). */
    public void show() {
        String path = currentPath();
        if (path.isEmpty() || "/".equals(path)) {
            navigate(homeFor(currentUser.get()));
            return;
        }
        String[] parts = split(path);
        for (int i = 0; i < patterns.size(); i++) {
            Map<String, String> params = match(patterns.get(i), parts);
            if (params != null) {
                Page page = factories.get(i).create(params);
                if (!allowed(page)) {
                    User user = currentUser.get();
                    navigate(user == null ? SIGN_IN : user.getHomeRoute());
                    return;
                }
                swap(page);
                return;
            }
        }
        navigate(fallbackRoute);
    }

    private boolean allowed(Page page) {
        if (page.getAllowedRoles().isEmpty()) {
            return true;
        }
        User user = currentUser.get();
        if (user == null) {
            return false;
        }
        Role role = user.getRole();
        return page.getAllowedRoles().contains(role);
    }

    private String homeFor(User user) {
        return user == null ? SIGN_IN : user.getHomeRoute();
    }

    private void swap(Page page) {
        if (current != null) {
            current.unmount();
        }
        current = page;
        Dom.clear(outlet);
        page.mount(outlet);
        HTMLDocument.current().setTitle(page.getTitle().isEmpty() ? APP_NAME : page.getTitle() + " · " + APP_NAME);
        Window.current().scrollTo(0, 0);
    }

    private static String[] split(String path) {
        String p = path.startsWith("#") ? path.substring(1) : path;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? new String[0] : p.split("/");
    }

    private static Map<String, String> match(String[] pattern, String[] parts) {
        if (pattern.length != parts.length) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i].startsWith(":")) {
                params.put(pattern[i].substring(1), parts[i]);
            } else if (!pattern[i].equals(parts[i])) {
                return null;
            }
        }
        return params;
    }
}
