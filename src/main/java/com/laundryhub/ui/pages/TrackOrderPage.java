package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.service.OrderTimeline;
import com.laundryhub.ui.BackHeader;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.GoogleMaps;
import com.laundryhub.ui.IconButton;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TextUtils;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 07: track an order. Map with shop, rider (live GPS from riders.last_lat/last_lng) and
 * customer markers, rider card with a call button, and the status timeline from order_status_history.
 * Refreshes every 10 seconds while open and stops when the user leaves.
 */
public final class TrackOrderPage extends Page {

    private static final int REFRESH_MILLIS = 10000;
    private static final int MAP_PADDING = 48;
    private static final int ZOOM_SINGLE = 15;

    private final AppContext ctx;
    private final long orderId;
    private int timer = -1;

    private Order order;
    private List<StatusEvent> events = Collections.emptyList();
    private JsonObject rider;
    private ShopSettings settings;

    private HTMLElement body;
    private HTMLElement mapBox;
    private HTMLElement mapChip;
    private HTMLElement riderSlot;
    private HTMLElement timelineSlot;
    private HTMLElement paymentSlot;
    private HTMLElement statusBadgeSlot;
    private GoogleMaps.GMap map;
    private GoogleMaps.Marker riderMarker;
    private GoogleMaps.Overlay route;
    private boolean mapFitted;

    /**
     * @param ctx     app context
     * @param orderId order id from the route
     */
    public TrackOrderPage(AppContext ctx, long orderId) {
        this.ctx = ctx;
        this.orderId = orderId;
    }

    @Override
    public String getTitle() {
        return "Track order";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected HTMLElement render() {
        body = Dom.div("page page--column track");
        body.appendChild(new BackHeader("Order", "Track your laundry", () -> ctx.navigate("#/orders")).getElement());
        body.appendChild(new Spinner("Loading your order…").getElement());
        return new CustomerShell("#/orders", ctx.getCurrentUser() == null ? null : ctx.getCurrentUser().getDisplayName(),
                ctx::signOut, body, true).getElement();
    }

    @Override
    protected void onMount() {
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog catalog) {
                settings = catalog.getSettings();
                refresh(true);
            }

            @Override
            public void onError(ApiException error) {
                refresh(true);
            }
        });
        timer = Window.setInterval(() -> refresh(false), REFRESH_MILLIS);
    }

    @Override
    protected void onUnmount() {
        if (timer >= 0) {
            Window.clearInterval(timer);
            timer = -1;
        }
    }

    /** Loads order → history → rider, then updates the screen. */
    private void refresh(boolean first) {
        ctx.getOrders().findById(orderId, new Callback<Order>() {
            @Override
            public void onSuccess(Order result) {
                order = result;
                ctx.getOrders().findHistory(orderId, new Callback<List<StatusEvent>>() {
                    @Override
                    public void onSuccess(List<StatusEvent> list) {
                        events = list;
                        loadRider(first);
                    }

                    @Override
                    public void onError(ApiException error) {
                        loadRider(first);
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                if (first) {
                    showError(error.getMessage());
                }
            }
        });
    }

    private void loadRider(boolean first) {
        if (order.getRiderId() == null) {
            rider = null;
            draw(first);
            return;
        }
        ctx.getRiderJobs().findRiderForTracking(order.getRiderId(), new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                rider = result;
                draw(first);
            }

            @Override
            public void onError(ApiException error) {
                rider = null;
                draw(first);
            }
        });
    }

    private void showError(String message) {
        onUnmount();
        Dom.clear(body);
        body.appendChild(new BackHeader("Order", "Track your laundry", () -> ctx.navigate("#/orders")).getElement());
        body.appendChild(new EmptyState("Couldn't load this order", message,
                new Button("Back to my orders", Button.Variant.PRIMARY, () -> ctx.navigate("#/orders"))).getElement());
    }

    // ---- drawing --------------------------------------------------------------------

    private void draw(boolean first) {
        if (first || riderSlot == null) {
            buildLayout();
        }
        statusBadgeSlot.replaceChild(Badge.of(order.getStatus()).getElement(), statusBadgeSlot.getFirstChild());
        drawRider();
        drawTimeline();
        drawPayment();
        drawMap();
    }

    private void buildLayout() {
        Dom.clear(body);
        body.appendChild(new BackHeader("Order #" + order.getOrderNo(), "Track your laundry",
                () -> ctx.navigate("#/orders")).getElement());

        statusBadgeSlot = Dom.div("track__badge", Dom.el("span", null));
        body.appendChild(statusBadgeSlot);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            body.appendChild(Dom.text("div", "notice notice--warn", "This order was cancelled. Contact the shop if you have questions."));
        }

        HTMLElement left = Dom.div("track__col");
        HTMLElement right = Dom.div("track__col");
        if (order.isDelivery()) {
            mapChip = Dom.text("span", "map-box__chip", "");
            mapBox = Dom.div("map-box track__map", Dom.div("map-box__canvas"), mapChip);
            left.appendChild(mapBox);
        } else {
            left.appendChild(shopCard());
        }
        riderSlot = Dom.div("track__rider");
        if (order.isDelivery()) {
            left.appendChild(riderSlot);
        }
        timelineSlot = Dom.div("card track__timeline");
        paymentSlot = Dom.div("card");
        Dom.append(right, timelineSlot, paymentSlot);
        body.appendChild(Dom.div("track__grid", left, right));
        mapFitted = false;
        map = null;
        riderMarker = null;
        route = null;
    }

    private HTMLElement shopCard() {
        String address = settings == null || settings.getShopAddress().isEmpty() ? "[SHOP ADDRESS]" : settings.getShopAddress();
        String hours = settings == null || settings.getShopHours().isEmpty() ? "[SHOP HOURS]" : settings.getShopHours();
        return Dom.div("card info-card", Dom.icon(Icons.PIN, "info-card__icon"),
                Dom.div("info-card__text",
                        Dom.text("span", "info-card__title", order.getStatus() == OrderStatus.READY
                                ? "Ready for pickup at Laundry HUB" : "Drop off at Laundry HUB"),
                        Dom.text("span", "info-card__line", address),
                        Dom.text("span", "info-card__line", "Open " + hours + " · " + order.getTimeSlot())));
    }

    private void drawRider() {
        if (riderSlot == null) {
            return;
        }
        Dom.clear(riderSlot);
        if (rider == null) {
            String text = order.getStatus().isClosed() ? "No rider assigned."
                    : "Waiting for a rider to accept your booking. This page updates by itself.";
            riderSlot.appendChild(Dom.div("card rider-card rider-card--empty", Dom.text("span", "muted", text)));
            return;
        }
        String name = TextUtils.orEmpty(rider.getString("full_name"));
        String plate = rider.getString("plate_no");
        HTMLElement info = Dom.div("rider-card__info",
                Dom.text("span", "rider-card__name", name.isEmpty() ? "Your rider" : name),
                Dom.text("span", "rider-card__sub", "Your rider" + (plate == null || plate.isEmpty() ? "" : " · " + plate)));
        HTMLElement card = Dom.div("card rider-card", Dom.text("span", "rider-card__avatar", TextUtils.initials(name)), info);
        String mobile = rider.getString("mobile");
        if (Validators.hasText(mobile)) {
            HTMLElement call = new IconButton(Icons.PHONE, "Call rider", true, () -> Browser.go("tel:" + mobile)).getElement();
            card.appendChild(call);
        }
        riderSlot.appendChild(card);
    }

    private void drawTimeline() {
        Dom.clear(timelineSlot);
        timelineSlot.appendChild(Dom.text("h2", "card__title", "Order status"));
        List<OrderTimeline.Step> steps = OrderTimeline.build(order, events);
        HTMLElement ol = Dom.el("ol", "timeline");
        for (int i = 0; i < steps.size(); i++) {
            OrderTimeline.Step step = steps.get(i);
            HTMLElement dot = Dom.el("span", "timeline__dot");
            if (step.getState() == OrderTimeline.State.DONE) {
                dot.appendChild(Dom.icon(Icons.CHECK.replace("width=\"18\" height=\"18\"", "width=\"14\" height=\"14\"")
                        .replace("stroke-width=\"2\"", "stroke-width=\"3\""), null));
            }
            HTMLElement text = Dom.div("timeline__text", Dom.text("span", "timeline__label", step.getLabel()),
                    Dom.text("span", "timeline__note", step.getNote()
                            + (step.getTime().isEmpty() ? "" : " · " + step.getTime())));
            HTMLElement li = Dom.el("li", "timeline__step timeline__step--" + step.getState().name().toLowerCase()
                    + (i == steps.size() - 1 ? " timeline__step--last" : ""));
            if (step.getState() == OrderTimeline.State.CURRENT) {
                li.setAttribute("aria-current", "step");
            }
            Dom.append(li, dot, text);
            ol.appendChild(li);
        }
        timelineSlot.appendChild(ol);
    }

    private void drawPayment() {
        Dom.clear(paymentSlot);
        paymentSlot.appendChild(Dom.text("h2", "card__title", "Payment"));
        paymentSlot.appendChild(row(order.getServiceType().getDisplayName() + " · " + order.getKg() + " kg · "
                + order.getSoapName(), MoneyFormatter.format(order.getTotal())));
        paymentSlot.appendChild(row("Payment", order.getCustomerPaymentLabel()));
        if (order.getGcashReference() != null) {
            paymentSlot.appendChild(row("GCash reference", order.getGcashReference()));
        }
        if (order.getPaymentStatus() == PaymentStatus.REJECTED) {
            paymentSlot.appendChild(Dom.text("div", "note note--warn",
                    "The shop could not match your GCash payment. Check the amount and send the correct reference number."));
            TextInput ref = new TextInput("New GCash reference number", "13-digit reference no.", "text", "numeric");
            Button resend = new Button("Resend reference", Button.Variant.PRIMARY, null).block(true);
            resend.setOnClick(() -> {
                if (!Validators.isGcashReference(ref.getValue())) {
                    ref.setError("The reference number has 13 digits.");
                    return;
                }
                resend.setLoading(true);
                ctx.getOrders().resubmitGcash(order.getId(), ref.getValue(), new Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.success("Sent! The shop will check it again.");
                        refresh(false);
                    }

                    @Override
                    public void onError(ApiException error) {
                        resend.setLoading(false);
                        ref.setError(error.getMessage());
                    }
                });
            });
            Dom.append(paymentSlot, ref.getElement(), resend.getElement());
        }
    }

    private static HTMLElement row(String label, String value) {
        return Dom.div("summary-row", Dom.text("span", "summary-row__label", label),
                Dom.text("span", "summary-row__value", value));
    }

    // ---- map ------------------------------------------------------------------------

    private void drawMap() {
        if (mapBox == null) {
            return;
        }
        Location customer = order.getLocation();
        Location riderAt = riderLocation();
        mapChip.setTextContent(chipText(riderAt != null));
        if (map == null && !mapBox.querySelector(".map-box__canvas").getClassList().contains("map-box__art")) {
            GoogleMaps.whenReady(() -> {
                if (map == null) {
                    createMap();
                }
                updateMap();
            }, this::mapFallback);
            return;
        }
        updateMap();
        if (customer == null) {
            mapChip.setTextContent("No pin for this order");
        }
    }

    private String chipText(boolean riderVisible) {
        switch (order.getStatus()) {
            case OUT_FOR_DELIVERY:
                return riderVisible ? "Rider is on the way · live GPS" : "Out for delivery";
            case PENDING:
                return riderVisible ? "Rider is coming to pick up · live GPS" : "Waiting for pickup";
            case COMPLETED:
                return "Delivered";
            default:
                return order.getStatus().getLabel();
        }
    }

    private Location riderLocation() {
        if (rider == null || rider.isNull("last_lat") || rider.isNull("last_lng") || order.getStatus().isClosed()) {
            return null;
        }
        return new Location(rider.getDouble("last_lat", 0), rider.getDouble("last_lng", 0), "");
    }

    private void createMap() {
        HTMLElement canvas = mapBox.querySelector(".map-box__canvas");
        Location center = order.getLocation() != null ? order.getLocation()
                : settings != null && settings.getShopLocation() != null ? settings.getShopLocation() : null;
        if (center == null) {
            mapFallback();
            return;
        }
        map = GoogleMaps.createMap(canvas, center.getLat(), center.getLng(), ZOOM_SINGLE);
        GoogleMaps.watchAuthFailure(() -> {
            map = null;
            riderMarker = null;
            route = null;
            mapFallback();
        });
        if (settings != null && settings.getShopLocation() != null) {
            Location shop = settings.getShopLocation();
            GoogleMaps.createMarker(map, shop.getLat(), shop.getLng(), GoogleMaps.MarkerStyle.SHOP, false, "Laundry HUB");
        }
        if (order.getLocation() != null) {
            GoogleMaps.createMarker(map, order.getLocation().getLat(), order.getLocation().getLng(),
                    GoogleMaps.MarkerStyle.PIN, false, "Your pin");
        }
    }

    private void updateMap() {
        if (map == null) {
            return;
        }
        Location riderAt = riderLocation();
        if (riderAt != null) {
            if (riderMarker == null) {
                riderMarker = GoogleMaps.createMarker(map, riderAt.getLat(), riderAt.getLng(),
                        GoogleMaps.MarkerStyle.RIDER, false, "Rider");
            } else {
                riderMarker.setPosition(GoogleMaps.latLng(riderAt.getLat(), riderAt.getLng()));
            }
        } else if (riderMarker != null) {
            riderMarker.setVisible(false);
        }
        List<Location> points = new ArrayList<>();
        if (settings != null && settings.getShopLocation() != null) {
            points.add(settings.getShopLocation());
        }
        if (riderAt != null) {
            points.add(riderAt);
        }
        if (order.getLocation() != null) {
            points.add(order.getLocation());
        }
        double[] lats = new double[points.size()];
        double[] lngs = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            lats[i] = points.get(i).getLat();
            lngs[i] = points.get(i).getLng();
        }
        if (route != null) {
            route.setMap(null);
        }
        if (points.size() > 1) {
            route = GoogleMaps.createDashedLine(map, lats, lngs);
            if (!mapFitted) {
                GoogleMaps.fitPoints(map, lats, lngs, MAP_PADDING);
                mapFitted = true;
            }
        }
    }

    private void mapFallback() {
        HTMLElement canvas = mapBox.querySelector(".map-box__canvas");
        Dom.clear(canvas);
        canvas.getClassList().add("map-box__art");
        Location riderAt = riderLocation();
        String text = order.getLocation() == null ? "Map not available"
                : "Your pin: " + order.getLocation().formatCoordinates()
                + (riderAt == null ? "" : " · Rider: " + riderAt.formatCoordinates());
        canvas.appendChild(Dom.text("div", "map-box__fallback", text));
    }
}
