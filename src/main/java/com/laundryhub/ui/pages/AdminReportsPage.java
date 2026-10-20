package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Order;
import com.laundryhub.model.Role;
import com.laundryhub.service.CsvExporter;
import com.laundryhub.service.ReportPeriod;
import com.laundryhub.ui.AdminShell;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.SegmentedControl;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.web.AppContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen A5: cash vs GCash sales, delivery vs walk-in, sales by fabric soap (bars), and
 * "Export CSV" (built in Java, saved with a Blob link). Period: today / 7 days / 30 days / this month.
 */
public final class AdminReportsPage extends Page {

    private static final int PERCENT = 100;

    private final AppContext ctx;
    private ReportPeriod period = ReportPeriod.THIS_MONTH;
    private HTMLElement body;
    private Button export;

    /**
     * @param ctx app context
     */
    public AdminReportsPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Sales reports";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.ADMIN, Role.STAFF);
    }

    @Override
    protected HTMLElement render() {
        List<String> labels = new ArrayList<>();
        for (ReportPeriod p : ReportPeriod.values()) {
            labels.add(p.getLabel());
        }
        SegmentedControl periods = new SegmentedControl(labels, period.ordinal(), i -> {
            period = ReportPeriod.values()[i];
            load();
        }).compact(true);
        body = Dom.div("admin-stack");
        export = new Button("Export CSV", Button.Variant.OUTLINE, Icons.DOWNLOAD, false, this::exportCsv);
        HTMLElement page = Dom.div("admin-stack", periods.getElement(), body, Dom.div("row", export.getElement()));
        load();
        return new AdminShell(ctx, "#/admin/reports", "Insights", "Sales reports", page).getElement();
    }

    private String from() {
        return period.from(System.currentTimeMillis());
    }

    private String to() {
        return period.to(System.currentTimeMillis());
    }

    private void load() {
        Dom.clear(body);
        body.appendChild(new Spinner("Loading sales…").getElement());
        ctx.getAdmin().loadReports(from(), to(), new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject r) {
                draw(r);
            }

            @Override
            public void onError(ApiException error) {
                Dom.clear(body);
                body.appendChild(new EmptyState("Couldn't load sales", error.getMessage(),
                        new Button("Try again", Button.Variant.PRIMARY, () -> load())).getElement());
            }
        });
    }

    private void draw(JsonObject r) {
        Dom.clear(body);
        HTMLElement kpis = Dom.div("kpis kpis--3",
                kpi("Cash sales", MoneyFormatter.format(r.getCentavos("cash_sales"))),
                kpi("GCash sales", MoneyFormatter.format(r.getCentavos("gcash_sales"))),
                kpi("Delivery vs walk-in", r.getLong("delivery_count", 0) + " / " + r.getLong("walkin_count", 0)));

        List<JsonObject> soaps = r.getArray("by_soap").objects();
        long max = 0;
        for (JsonObject s : soaps) {
            max = Math.max(max, s.getCentavos("total"));
        }
        HTMLElement bars = Dom.div("bars");
        for (JsonObject s : soaps) {
            long total = s.getCentavos("total");
            HTMLElement fill = Dom.el("span", "bar__fill");
            fill.getStyle().setProperty("width", (max == 0 ? 0 : total * PERCENT / max) + "%");
            HTMLElement track = Dom.div("bar__track", fill);
            track.setAttribute("role", "img");
            track.setAttribute("aria-label", s.getString("name") + ": " + MoneyFormatter.format(total));
            bars.appendChild(Dom.div("bar", Dom.text("span", "bar__label", s.getString("name")), track,
                    Dom.text("span", "bar__value", MoneyFormatter.format(total))));
        }
        HTMLElement soapCard = Dom.div("card admin-card", Dom.text("h2", "card__title", "Sales by fabric soap"),
                soaps.isEmpty() ? Dom.text("p", "muted", "No soaps yet.") : bars);
        HTMLElement summary = Dom.text("p", "muted", r.getLong("order_count", 0) + " orders · "
                + r.getLong("kilos", 0) + " kg · total " + MoneyFormatter.format(r.getCentavos("total_sales"))
                + " · " + from() + " to " + to() + " (cancelled orders excluded)");
        Dom.append(body, kpis, soapCard, summary);
    }

    private static HTMLElement kpi(String label, String value) {
        return Dom.div("kpi", Dom.text("span", "kpi__label", label), Dom.text("span", "kpi__value kpi__value--md", value));
    }

    private void exportCsv() {
        export.setLoading(true);
        String from = from();
        String to = to();
        ctx.getOrders().findBetween(from, to, new Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                export.setLoading(false);
                if (orders.isEmpty()) {
                    Toast.info("No orders in this period.");
                    return;
                }
                Browser.download("laundryhub-sales-" + from + "-to-" + to + ".csv",
                        CsvExporter.ordersToCsv(orders), "text/csv;charset=utf-8");
                Toast.success("Exported " + orders.size() + " orders.");
            }

            @Override
            public void onError(ApiException error) {
                export.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }
}
