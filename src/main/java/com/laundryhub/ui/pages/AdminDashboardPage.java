package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.Role;
import com.laundryhub.model.ServiceType;
import com.laundryhub.ui.AdminShell;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.InstallCard;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Spinner;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.web.AppContext;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen A1: summary cards, order pipeline and recent orders (rpc admin_dashboard).
 */
public final class AdminDashboardPage extends Page {

    private static final OrderStatus[] PIPELINE = {
        OrderStatus.PENDING, OrderStatus.WASHING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.COMPLETED
    };

    private final AppContext ctx;
    private HTMLElement body;

    /**
     * @param ctx app context
     */
    public AdminDashboardPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Dashboard";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.ADMIN, Role.STAFF);
    }

    @Override
    protected HTMLElement render() {
        body = Dom.div("admin-stack");
        body.appendChild(new Spinner("Loading today’s overview…").getElement());
        load();
        return new AdminShell(ctx, "#/admin", "Today’s overview", "Dashboard", body).getElement();
    }

    private void load() {
        ctx.getAdmin().loadDashboard(new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject d) {
                draw(d);
            }

            @Override
            public void onError(ApiException error) {
                Dom.clear(body);
                body.appendChild(new EmptyState("Couldn't load the dashboard", error.getMessage(),
                        new Button("Try again", Button.Variant.PRIMARY, () -> rerender())).getElement());
            }
        });
    }

    private void draw(JsonObject d) {
        Dom.clear(body);
        HTMLElement kpis = Dom.div("kpis",
                kpi("Orders today", Long.toString(d.getLong("orders_today", 0)), false),
                kpi("Waiting to process", Long.toString(d.getLong("waiting_to_process", 0)), false),
                kpi("Kilos in today", d.getLong("kilos_today", 0) + " kg", false),
                kpi("Sales today", MoneyFormatter.format(d.getCentavos("sales_today")), true));

        JsonObject pipeline = d.getObject("pipeline");
        HTMLElement tiles = Dom.div("pipeline");
        for (OrderStatus s : PIPELINE) {
            long count = pipeline == null ? 0 : pipeline.getLong(s.name(), 0);
            HTMLElement tile = Dom.el("a", "pipeline__tile pipeline__tile--" + s.getBadgeKey());
            tile.setAttribute("href", "#/admin/orders");
            Dom.append(tile, Dom.text("span", "pipeline__label", s.getLabel()),
                    Dom.text("span", "pipeline__count", Long.toString(count)));
            tiles.appendChild(tile);
        }
        HTMLElement pipelineCard = Dom.div("card admin-card", Dom.text("h2", "card__title", "Order pipeline"), tiles);

        HTMLElement table = Dom.el("table", "admin-table");
        HTMLElement head = Dom.el("tr", null);
        for (String h : new String[] {"Order", "Customer", "Service", "Kg", "Payment", "Total", "Status", ""}) {
            head.appendChild(Dom.text("th", null, h));
        }
        HTMLElement thead = Dom.el("thead", null);
        thead.appendChild(head);
        HTMLElement tbody = Dom.el("tbody", null);
        for (JsonObject o : d.getArray("recent").objects()) {
            HTMLElement tr = Dom.el("tr", null);
            ServiceType service = ServiceType.fromCode(o.getString("service_type"));
            long id = o.getLong("id", 0);
            Button open = new Button("Open", Button.Variant.OUTLINE, () -> {
                ctx.setAdminSelectedOrderId(id);
                ctx.setAdminSearch("");
                ctx.navigate("#/admin/orders");
            });
            open.getElement().getClassList().add("btn--small");
            Dom.append(tr,
                    Dom.text("td", "admin-table__strong", o.getString("order_no")),
                    Dom.text("td", null, o.getString("customer_name", "")),
                    Dom.text("td", null, service == null ? "" : service.getDisplayName()),
                    Dom.text("td", null, Long.toString(o.getLong("kg", 0))),
                    Dom.text("td", null, "GCASH".equals(o.getString("payment_method")) ? "GCash" : "Cash"),
                    Dom.text("td", "admin-table__strong", MoneyFormatter.format(o.getCentavos("total"))),
                    cell(Badge.of(OrderStatus.fromCode(o.getString("status"))).getElement()),
                    cell(open.getElement()));
            tbody.appendChild(tr);
        }
        Dom.append(table, thead, tbody);
        HTMLElement recentHead = Dom.div("card__head", Dom.text("h2", "card__title", "Recent orders"),
                new Button("View all", Button.Variant.LINK, () -> {
                    ctx.setAdminSearch("");
                    ctx.navigate("#/admin/orders");
                }).getElement());
        HTMLElement recent = Dom.div("card admin-card", recentHead,
                d.getArray("recent").isEmpty()
                        ? Dom.text("p", "muted", "No orders yet.")
                        : Dom.div("table-scroll", table));
        Dom.append(body, new InstallCard().getElement(), kpis, pipelineCard, recent);
    }

    private static HTMLElement kpi(String label, String value, boolean accent) {
        return Dom.div(accent ? "kpi kpi--accent" : "kpi", Dom.text("span", "kpi__label", label),
                Dom.text("span", "kpi__value", value));
    }

    private static HTMLElement cell(HTMLElement child) {
        HTMLElement td = Dom.el("td", null);
        td.appendChild(child);
        return td;
    }
}
