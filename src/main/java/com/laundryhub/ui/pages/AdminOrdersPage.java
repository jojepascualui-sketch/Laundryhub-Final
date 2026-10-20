package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.service.AdminOrderFilter;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.ui.AdminShell;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Chip;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.IconButton;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.JobMap;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TextUtils;
import com.laundryhub.web.AppContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

/**
 * Screen A2: orders with status filter chips, list on the left and a detail panel on the right
 * (map of the pin, status buttons, assign rider, price breakdown, GCash verify / reject).
 * Phones: the detail replaces the list, with a back button.
 */
public final class AdminOrdersPage extends Page {

    private static final int REFRESH_MILLIS = 30000;
    private static final OrderStatus[] STATUS_BUTTONS = {
        OrderStatus.PENDING, OrderStatus.WASHING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.COMPLETED
    };

    private final AppContext ctx;
    private List<Order> orders;
    private OrderStatus filterStatus;
    private JsonArray riders = new JsonArray();
    private ShopSettings settings;
    private int timer = -1;

    private HTMLElement root;
    private HTMLElement chips;
    private HTMLElement list;
    private HTMLElement detail;
    private String detailKey;

    /**
     * @param ctx app context
     */
    public AdminOrdersPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Orders";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.ADMIN, Role.STAFF);
    }

    @Override
    protected HTMLElement render() {
        chips = Dom.div("row row--wrap admin-chips");
        list = Dom.div("stack admin-orders__list");
        detail = Dom.div("admin-orders__detail");
        root = Dom.div("admin-orders", chips, Dom.div("admin-orders__grid", list, detail));
        list.appendChild(new Spinner("Loading orders…").getElement());
        return new AdminShell(ctx, "#/admin/orders", "Manage and update", "Orders", root).getElement();
    }

    @Override
    protected void onMount() {
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog c) {
                settings = c.getSettings();
            }

            @Override
            public void onError(ApiException error) {
                // optional (shop marker only)
            }
        });
        ctx.getAdmin().loadUsers(Role.RIDER, new Callback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray result) {
                riders = result;
                drawDetail();
            }

            @Override
            public void onError(ApiException error) {
                riders = new JsonArray();
            }
        });
        load(true);
        timer = Window.setInterval(() -> load(false), REFRESH_MILLIS);
    }

    @Override
    protected void onUnmount() {
        if (timer >= 0) {
            Window.clearInterval(timer);
        }
    }

    private void load(boolean first) {
        ctx.getOrders().findAll(null, null, new Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                orders = result;
                drawChips();
                drawList();
                if (first) {
                    drawDetail();
                } else {
                    refreshDetailIfChanged();
                }
            }

            @Override
            public void onError(ApiException error) {
                if (first) {
                    Dom.clear(list);
                    list.appendChild(new EmptyState("Couldn't load orders", error.getMessage(),
                            new Button("Try again", Button.Variant.PRIMARY, () -> load(true))).getElement());
                }
            }
        });
    }

    private Order selected() {
        if (orders == null) {
            return null;
        }
        for (Order o : orders) {
            if (o.getId() == ctx.getAdminSelectedOrderId()) {
                return o;
            }
        }
        return null;
    }

    private void select(long id) {
        ctx.setAdminSelectedOrderId(id);
        drawList();
        drawDetail();
        Dom.toggleClass(root, "has-selection", id >= 0);
        root.scrollIntoView();
    }

    // ---- chips & list ---------------------------------------------------------------

    private void drawChips() {
        AdminOrderFilter filter = new AdminOrderFilter(orders);
        Map<OrderStatus, Integer> counts = filter.countByStatus();
        Dom.clear(chips);
        addChip("All (" + filter.total() + ")", null);
        for (OrderStatus s : AdminOrderFilter.CHIP_STATUSES) {
            int n = counts.get(s);
            if (s == OrderStatus.CANCELLED && n == 0) {
                continue;
            }
            addChip(s.getLabel() + " (" + n + ")", s);
        }
        if (!ctx.getAdminSearch().isEmpty()) {
            Chip clear = new Chip(Chip.Style.FILTER, "Search: “" + ctx.getAdminSearch() + "” ✕", () -> {
                ctx.setAdminSearch("");
                ctx.getRouter().show();
            });
            chips.appendChild(clear.getElement());
        }
    }

    private void addChip(String label, OrderStatus status) {
        Chip chip = new Chip(Chip.Style.FILTER, label, () -> {
            filterStatus = status;
            drawChips();
            drawList();
        });
        chip.setSelected(status == filterStatus);
        chips.appendChild(chip.getElement());
    }

    private void drawList() {
        Dom.clear(list);
        List<Order> shown = new AdminOrderFilter(orders).filter(filterStatus, ctx.getAdminSearch());
        if (shown.isEmpty()) {
            list.appendChild(new EmptyState(null, "No orders match this filter.", null).getElement());
            return;
        }
        for (Order o : shown) {
            HTMLElement card = Dom.el("button", o.getId() == ctx.getAdminSelectedOrderId()
                    ? "admin-order-card is-selected" : "admin-order-card");
            card.setAttribute("type", "button");
            Dom.append(card,
                    Dom.div("order-card__top", Dom.text("span", "order-card__no", o.getOrderNo()), Badge.of(o.getStatus()).getElement()),
                    Dom.text("div", "admin-order-card__name", o.getCustomerName()),
                    Dom.text("div", "admin-order-card__line", o.getServiceType().getDisplayName() + " · " + o.getKg() + " kg · "
                            + MoneyFormatter.format(o.getTotal())));
            if (o.getPaymentStatus() == PaymentStatus.PENDING_VERIFICATION) {
                card.appendChild(Dom.text("div", "admin-order-card__flag", "GCash to verify"));
            }
            Dom.onClick(card, () -> select(o.getId()));
            list.appendChild(card);
        }
    }

    // ---- detail ---------------------------------------------------------------------

    private void refreshDetailIfChanged() {
        Order o = selected();
        String key = o == null ? "" : o.getStatus() + "|" + o.getPaymentStatus() + "|" + o.getRiderId();
        if (!key.equals(detailKey)) {
            drawDetail();
        }
    }

    private void drawDetail() {
        if (detail == null || orders == null) {
            return;
        }
        Dom.clear(detail);
        Order o = selected();
        Dom.toggleClass(root, "has-selection", o != null);
        if (o == null) {
            detailKey = "";
            detail.appendChild(new EmptyState("Select an order", "Choose an order on the left to see its details.", null).getElement());
            return;
        }
        detailKey = o.getStatus() + "|" + o.getPaymentStatus() + "|" + o.getRiderId();

        HTMLElement back = new IconButton(Icons.BACK, "Back to the list", false, () -> select(-1)).getElement();
        back.getClassList().add("admin-detail__back");
        HTMLElement head = Dom.div("admin-detail__head",
                back,
                Dom.div("admin-detail__who",
                        Dom.text("h2", "admin-detail__no", o.getOrderNo()),
                        Dom.text("div", "muted", o.getCustomerName()
                                + (o.getCustomerMobile() == null ? "" : " · " + TextUtils.formatMobile(o.getCustomerMobile())))),
                new Badge(o.getStatus().getLabel(), Badge.toneFor(o.getStatus()), true).getElement());

        HTMLElement tiles = Dom.div("info-tiles",
                infoTile("Service", o.getServiceType().getDisplayName()),
                infoTile("Weight", o.getKg() + " kg"),
                infoTile("Fabric soap", o.getSoapName()),
                infoTile("Schedule", o.getTimeSlot()));

        HTMLElement card = Dom.div("card admin-detail", head, tiles, locationSection(o), statusSection(o));
        if (o.isDelivery()) {
            card.appendChild(riderSection(o));
        }
        card.appendChild(priceSection(o));
        card.appendChild(paymentSection(o));
        detail.appendChild(card);
    }

    private static HTMLElement infoTile(String label, String value) {
        return Dom.div("info-tile", Dom.text("span", "info-tile__label", label), Dom.text("span", "info-tile__value", value));
    }

    private HTMLElement locationSection(Order o) {
        HTMLElement box = Dom.div("stack stack--tight");
        box.appendChild(Dom.text("h3", "section-title", "Pinned location"));
        Location pin = o.getLocation();
        if (!o.isDelivery() || pin == null) {
            box.appendChild(Dom.text("div", "notice notice--info",
                    "Walk-in order: the customer drops off and picks up the laundry at the shop."));
            return box;
        }
        Location shop = settings == null ? null : settings.getShopLocation();
        HTMLElement mapWrap = Dom.div("admin-map", new JobMap(shop, pin, null, pin).getElement());
        HTMLElement link = Dom.text("a", "link-button", "Open in Google Maps");
        link.setAttribute("href", "https://www.google.com/maps/search/?api=1&query=" + pin.getLat() + "," + pin.getLng());
        link.setAttribute("target", "_blank");
        link.setAttribute("rel", "noopener");
        String address = pin.getAddress().isEmpty() ? "" : pin.getAddress() + " · ";
        String details = o.getAddressDetails() == null ? "" : " · " + o.getAddressDetails();
        box.appendChild(mapWrap);
        box.appendChild(Dom.div("admin-map__foot", Dom.text("span", "muted", address + pin.formatCoordinates() + details), link));
        return box;
    }

    private HTMLElement statusSection(Order o) {
        HTMLElement row = Dom.div("row row--wrap");
        for (OrderStatus s : STATUS_BUTTONS) {
            if (s == OrderStatus.OUT_FOR_DELIVERY && !o.isDelivery()) {
                continue;
            }
            Button b = new Button(s.getLabel(), s == o.getStatus() ? Button.Variant.PRIMARY : Button.Variant.OUTLINE, null);
            b.getElement().getClassList().add("btn--small");
            b.setOnClick(() -> {
                if (s != o.getStatus()) {
                    changeStatus(o, s, b);
                }
            });
            row.appendChild(b.getElement());
        }
        HTMLElement box = Dom.div("stack stack--tight", Dom.text("h3", "section-title", "Update status"), row);
        if (!o.getStatus().isClosed()) {
            Button cancel = new Button("Cancel order", Button.Variant.LINK, null);
            cancel.setOnClick(() -> {
                if (Window.confirm("Cancel order " + o.getOrderNo() + "? Riders will no longer see its jobs.")) {
                    changeStatus(o, OrderStatus.CANCELLED, cancel);
                }
            });
            cancel.getElement().getClassList().add("btn--danger-link");
            box.appendChild(cancel.getElement());
        }
        return box;
    }

    private void changeStatus(Order o, OrderStatus status, Button button) {
        button.setLoading(true);
        ctx.getOrders().updateStatus(o.getId(), status, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.success(o.getOrderNo() + " is now " + status.getLabel().toLowerCase() + ".");
                load(true);
            }

            @Override
            public void onError(ApiException error) {
                button.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }

    private HTMLElement riderSection(Order o) {
        HTMLSelectElement select = (HTMLSelectElement) Dom.el("select", "select admin-rider-select");
        select.setAttribute("aria-label", "Assign rider");
        HTMLElement none = Dom.text("option", null, "Not assigned");
        none.setAttribute("value", "");
        select.appendChild(none);
        for (JsonObject r : riders.objects()) {
            if (!r.getBoolean("active", true)) {
                continue;
            }
            HTMLElement opt = Dom.text("option", null, r.getString("full_name", "Rider")
                    + (r.isNull("plate_no") ? "" : " · " + r.getString("plate_no"))
                    + (r.getBoolean("on_duty", false) ? " · on duty" : ""));
            opt.setAttribute("value", r.getString("id"));
            select.appendChild(opt);
        }
        select.setValue(o.getRiderId() == null ? "" : o.getRiderId());
        select.setDisabled(o.getStatus().isClosed());
        select.addEventListener("change", e -> {
            String value = select.getValue();
            ctx.getOrders().assignRider(o.getId(), value.isEmpty() ? null : value, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.success(value.isEmpty() ? "Rider removed." : "Rider assigned.");
                    load(true);
                }

                @Override
                public void onError(ApiException error) {
                    Toast.error(error.getMessage());
                    select.setValue(o.getRiderId() == null ? "" : o.getRiderId());
                }
            });
        });
        return Dom.div("stack stack--tight", Dom.text("h3", "section-title", "Assign rider"), select);
    }

    private HTMLElement priceSection(Order o) {
        PriceBreakdown p = o.getPriceBreakdown();
        HTMLElement box = Dom.div("stack stack--tight admin-price");
        box.appendChild(row("Laundry (" + o.getKg() + " kg × " + MoneyFormatter.formatShort(p.getRatePerKg()) + ")",
                MoneyFormatter.format(p.getLaundryCost())));
        box.appendChild(row(o.getSoapName() + " add-on", MoneyFormatter.format(p.getSoapCost())));
        if (o.isDelivery()) {
            box.appendChild(row("Delivery fee", MoneyFormatter.format(p.getDeliveryFee())));
        }
        box.appendChild(Dom.div("summary-total",
                Dom.text("span", "summary-total__label", "Total · " + o.getPaymentLabel() + " ("
                        + o.getPaymentStatus().getShortLabel() + ")"),
                Dom.text("span", "summary-total__value", MoneyFormatter.format(o.getTotal()))));
        return box;
    }

    private HTMLElement paymentSection(Order o) {
        HTMLElement box = Dom.div("stack stack--tight admin-payment");
        if (o.isCash()) {
            if (o.getPaymentStatus() != PaymentStatus.PAID && !o.getStatus().equals(OrderStatus.CANCELLED)) {
                Button paid = new Button("Mark cash as paid", Button.Variant.OUTLINE, null);
                paid.setOnClick(() -> {
                    paid.setLoading(true);
                    ctx.getOrders().markPaid(o.getId(), reloadAfter(paid, "Marked as paid."));
                });
                box.appendChild(paid.getElement());
            }
            return box;
        }
        box.appendChild(Dom.text("h3", "section-title", "GCash payment"));
        box.appendChild(row("Reference no.", TextUtils.orEmpty(o.getGcashReference())));
        box.appendChild(row("Status", o.getPaymentStatus().getCustomerLabel()));
        if (o.getGcashProofPath() != null) {
            HTMLElement proof = Dom.div("admin-proof", Dom.text("span", "muted", "Loading screenshot…"));
            box.appendChild(proof);
            ctx.getOrders().getProofUrl(o.getGcashProofPath(), new Callback<String>() {
                @Override
                public void onSuccess(String url) {
                    Dom.clear(proof);
                    HTMLElement a = Dom.el("a", null);
                    a.setAttribute("href", url);
                    a.setAttribute("target", "_blank");
                    a.setAttribute("rel", "noopener");
                    HTMLElement img = Dom.el("img", "admin-proof__img");
                    img.setAttribute("src", url);
                    img.setAttribute("alt", "GCash payment screenshot for " + o.getOrderNo());
                    a.appendChild(img);
                    proof.appendChild(a);
                }

                @Override
                public void onError(ApiException error) {
                    Dom.clear(proof);
                    proof.appendChild(Dom.text("span", "muted", "Screenshot could not be loaded: " + error.getMessage()));
                }
            });
        } else {
            box.appendChild(Dom.text("p", "muted", "No screenshot uploaded."));
        }
        if (o.getPaymentStatus() == PaymentStatus.PENDING_VERIFICATION || o.getPaymentStatus() == PaymentStatus.REJECTED) {
            Button verify = new Button("Verify payment", Button.Variant.PRIMARY, null);
            Button reject = new Button("Reject", Button.Variant.OUTLINE, null);
            verify.setOnClick(() -> {
                verify.setLoading(true);
                ctx.getOrders().verifyGcash(o.getId(), true, null, reloadAfter(verify, "GCash payment verified."));
            });
            reject.setOnClick(() -> {
                String note = Window.prompt("Reason for rejecting (the customer will be asked to resend):",
                        "Amount or reference number does not match");
                if (note == null) {
                    return;
                }
                reject.setLoading(true);
                ctx.getOrders().verifyGcash(o.getId(), false, note, reloadAfter(reject, "Payment rejected. The customer will be asked to resend."));
            });
            box.appendChild(Dom.div("row row--wrap", verify.getElement(),
                    o.getPaymentStatus() == PaymentStatus.REJECTED ? null : reject.getElement()));
        }
        return box;
    }

    private Callback<Void> reloadAfter(Button button, String message) {
        return new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.success(message);
                load(true);
            }

            @Override
            public void onError(ApiException error) {
                button.setLoading(false);
                Toast.error(error.getMessage());
            }
        };
    }

    private static HTMLElement row(String label, String value) {
        return Dom.div("summary-row", Dom.text("span", "summary-row__label", label),
                Dom.text("span", "summary-row__value", value));
    }
}
