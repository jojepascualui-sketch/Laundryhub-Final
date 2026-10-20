package com.laundryhub.ui;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.User;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.web.AppContext;
import java.util.Arrays;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Admin console layout (A1–A5): dark sidebar, then kicker + page title, search box and bell.
 * Phones/tablets: the sidebar becomes a top bar with a menu button.
 */
public class AdminShell extends Component {

    /** Admin pages in sidebar order. */
    public static final List<NavItem> NAV = Arrays.asList(
            new NavItem("Dashboard", "#/admin", null),
            new NavItem("Orders", "#/admin/orders", null),
            new NavItem("Pricing & soaps", "#/admin/pricing", null),
            new NavItem("Customers & riders", "#/admin/users", null),
            new NavItem("Sales reports", "#/admin/reports", null));

    private final AppContext ctx;
    private final String activeRoute;
    private final String kicker;
    private final String title;
    private final HTMLElement content;

    /**
     * @param ctx         app context
     * @param activeRoute current admin route
     * @param kicker      small line above the title, e.g. "Today’s overview"
     * @param title       page title, e.g. "Dashboard"
     * @param content     page content
     */
    public AdminShell(AppContext ctx, String activeRoute, String kicker, String title, HTMLElement content) {
        this.ctx = ctx;
        this.activeRoute = activeRoute;
        this.kicker = kicker;
        this.title = title;
        this.content = content;
    }

    @Override
    protected HTMLElement render() {
        User user = ctx.getCurrentUser();
        Sidebar sidebar = new Sidebar(NAV, activeRoute, user == null ? "" : user.getDisplayName(),
                user == null ? "" : user.getRoleLabel(), ctx::signOut);

        HTMLInputElement search = Dom.input("search", "admin-search");
        search.setPlaceholder("Search order no. or customer");
        search.setAttribute("aria-label", "Search orders");
        search.setValue(ctx.getAdminSearch());
        search.addEventListener("keydown", e -> {
            if ("Enter".equals(keyOf(e))) {
                ctx.setAdminSearch(search.getValue());
                ctx.setAdminSelectedOrderId(-1);
                if ("#/admin/orders".equals(activeRoute)) {
                    ctx.getRouter().show();
                } else {
                    ctx.navigate("#/admin/orders");
                }
            }
        });
        HTMLElement bell = new IconButton(Icons.BELL, "Notifications", false, this::notifications).getElement();

        HTMLElement head = Dom.div("admin-head",
                Dom.div("admin-head__titles", Dom.text("div", "admin-head__kicker", kicker),
                        Dom.text("h1", "admin-head__title", title)),
                Dom.div("admin-head__tools", search, bell));
        HTMLElement main = Dom.el("main", "admin-main");
        Dom.append(main, head, content);
        return Dom.div("admin", sidebar.getElement(), main);
    }

    private void notifications() {
        ctx.getAdmin().loadDashboard(new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject d) {
                long waiting = d.getLong("pending_gcash", 0);
                long pending = d.getLong("waiting_to_process", 0);
                if (waiting == 0 && pending == 0) {
                    Toast.info("No new notifications.");
                } else {
                    Toast.info(pending + " order(s) waiting to process · " + waiting + " GCash payment(s) to verify.");
                }
            }

            @Override
            public void onError(ApiException error) {
                Toast.error(error.getMessage());
            }
        });
    }

    @JSBody(params = "e", script = "return e.key || '';")
    private static native String keyOf(Event e);
}
