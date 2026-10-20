package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Booking;
import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.Rider;
import com.laundryhub.model.RiderJob;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.service.RiderBoard;
import com.laundryhub.ui.BookingCard;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.IconButton;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.JobMap;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.RiderLocationSharer;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TextUtils;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.List;
import java.util.Set;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screens R2 / R3 / R4: an accepted job. Map with "Navigate in Google Maps", customer card with a
 * call button, the step checklist and one big button whose label follows the step
 * (rpc advance_job on each tap). Cash return deliveries include "Cash received". When the last step
 * is done the "Job completed" screen is shown. Desktop: map left, details right.
 */
public final class RiderJobPage extends Page {

    private static final String NAVIGATE_ICON = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" "
            + "stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
            + "<path d=\"M3 11l18-8-8 18-2-8z\"/></svg>";

    private final AppContext ctx;
    private final long jobId;
    private Booking booking;
    private ShopSettings settings;
    private JobMap map;
    private Runnable stopGps;
    private HTMLElement root;

    /**
     * @param ctx   app context
     * @param jobId job id from the route
     */
    public RiderJobPage(AppContext ctx, long jobId) {
        this.ctx = ctx;
        this.jobId = jobId;
    }

    @Override
    public String getTitle() {
        return "Active job";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.RIDER);
    }

    @Override
    protected HTMLElement render() {
        root = Dom.div("rider-job");
        root.appendChild(Dom.div("page", new Spinner("Loading job…").getElement()));
        return root;
    }

    @Override
    protected void onMount() {
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog catalog) {
                settings = catalog.getSettings();
                load();
            }

            @Override
            public void onError(ApiException error) {
                load();
            }
        });
        if (((Rider) ctx.getCurrentUser()).isOnDuty()) {
            RiderLocationSharer.start(ctx.getRiderJobs());
        }
        stopGps = RiderLocationSharer.addListener(p -> {
            if (map != null) {
                map.setRider(p);
            }
        });
    }

    @Override
    protected void onUnmount() {
        if (stopGps != null) {
            stopGps.run();
        }
        if (!Window.current().getLocation().getHash().startsWith("#/rider")) {
            RiderLocationSharer.stop();
        }
    }

    private void load() {
        ctx.getRiderJobs().findJob(jobId, new Callback<Booking>() {
            @Override
            public void onSuccess(Booking result) {
                booking = result;
                if (result.getJob().getStatus() == JobStatus.COMPLETED) {
                    drawDone();
                } else if (!result.getJob().getStatus().isActive()
                        || !ctx.getCurrentUser().getId().equals(result.getJob().getRiderId())) {
                    drawMessage("This job is not yours", "Accept it from your bookings first.");
                } else {
                    drawJob();
                }
            }

            @Override
            public void onError(ApiException error) {
                drawMessage("Couldn't open this job", error.getMessage());
            }
        });
    }

    private void drawMessage(String title, String text) {
        Dom.clear(root);
        root.appendChild(Dom.div("page page--narrow", new EmptyState(title, text,
                new Button("Back to bookings", Button.Variant.DARK, () -> ctx.navigate("#/rider"))).getElement()));
    }

    // ---- R2 / R3 --------------------------------------------------------------------

    private void drawJob() {
        RiderJob job = booking.getJob();
        Order order = booking.getOrder();
        Location shop = settings == null ? null : settings.getShopLocation();
        Location destination = RiderBoard.destination(booking, shop);

        map = new JobMap(shop, order.getLocation(), RiderLocationSharer.getLatest(), destination);
        HTMLElement back = new IconButton(Icons.BACK, "Back to bookings", false, () -> ctx.navigate("#/rider")).getElement();
        back.getClassList().add("rider-job__back");
        HTMLElement mapArea = Dom.div("rider-job__map", map.getElement(), back);
        if (destination != null) {
            Button navigate = new Button("Navigate in Google Maps", Button.Variant.DARK, NAVIGATE_ICON, false,
                    () -> Browser.openNewTab(RiderBoard.directionsUrl(destination)));
            navigate.getElement().getClassList().add("rider-job__navigate");
            mapArea.appendChild(navigate.getElement());
        }

        String distance = RiderBoard.distanceLabel(booking, RiderLocationSharer.getLatest());
        HTMLElement head = Dom.div("booking__top", BookingCard.typeBadge(job).getElement(),
                Dom.text("span", "booking__meta", order.getOrderNo() + (distance.isEmpty() ? "" : " · " + distance)));

        HTMLElement customer = Dom.div("card rider-card",
                Dom.text("span", "rider-card__avatar", TextUtils.initials(order.getCustomerName())),
                Dom.div("rider-card__info",
                        Dom.text("span", "rider-card__name", order.getCustomerName()),
                        Dom.text("span", "rider-card__sub", BookingCard.addressLine(order))));
        if (Validators.hasText(order.getCustomerMobile())) {
            String tel = order.getCustomerMobile();
            customer.appendChild(new IconButton(Icons.PHONE, "Call customer", true, () -> Browser.go("tel:" + tel)).getElement());
        }

        HTMLElement checklist = Dom.el("ol", "checklist card");
        List<String> steps = job.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            RiderJob.StepState state = job.getStepState(i);
            HTMLElement dot = Dom.el("span", "timeline__dot");
            if (state == RiderJob.StepState.DONE) {
                dot.appendChild(Dom.icon(Icons.CHECK.replace("width=\"18\" height=\"18\"", "width=\"12\" height=\"12\"")
                        .replace("stroke-width=\"2\"", "stroke-width=\"3\""), null));
            }
            HTMLElement li = Dom.el("li", "timeline__step checklist__step timeline__step--" + state.name().toLowerCase()
                    + (i == steps.size() - 1 ? " timeline__step--last" : ""));
            Dom.append(li, dot, Dom.text("span", "checklist__label", steps.get(i)));
            checklist.appendChild(li);
        }

        boolean collectCash = RiderJob.TYPE_RETURN.equals(job.getType()) && job.isCashOrder();
        HTMLElement payment = Dom.div(collectCash ? "pay-box pay-box--warn" : "pay-box",
                Dom.text("span", "pay-box__note", job.getPaymentNote()),
                Dom.text("span", "pay-box__total", MoneyFormatter.format(order.getTotal())));

        Button action = new Button(job.getNextActionLabel(), Button.Variant.PRIMARY, null).block(true);
        action.getElement().getClassList().add("btn--tall");
        action.setOnClick(() -> advance(action));

        HTMLElement sheet = Dom.div("rider-job__sheet", head, customer, checklist, payment);
        HTMLElement footer = Dom.el("footer", "rider-job__footer");
        footer.appendChild(action.getElement());
        Dom.clear(root);
        Dom.append(root, mapArea, Dom.div("rider-job__panel", sheet, footer));
    }

    private void advance(Button action) {
        action.setLoading(true);
        ctx.getRiderJobs().advance(jobId, new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                if (result.getBoolean("done", false)) {
                    Toast.success("Job completed!");
                }
                load();
            }

            @Override
            public void onError(ApiException error) {
                action.setLoading(false);
                Toast.error(error.getMessage());
                if (error.isConflict()) {
                    load();
                }
            }
        });
    }

    // ---- R4 -------------------------------------------------------------------------

    private void drawDone() {
        RiderJob job = booking.getJob();
        Order order = booking.getOrder();
        map = null;
        HTMLElement check = Dom.icon(Icons.CHECK.replace("width=\"18\" height=\"18\"", "width=\"44\" height=\"44\"")
                .replace("stroke-width=\"2\"", "stroke-width=\"3\""), "placed__check");
        HTMLElement page = Dom.div("page page--narrow placed rider-done",
                Dom.div("placed__top", check,
                        Dom.text("h1", "placed__title", "Job completed"),
                        Dom.text("p", "placed__sub", order.getOrderNo() + " · " + job.getTypeLabel() + " for "
                                + order.getCustomerName())),
                Dom.div("card placed__card",
                        Dom.div("summary-row", Dom.text("span", "summary-row__label", job.getDoneNote()),
                                Dom.text("span", "summary-row__value", MoneyFormatter.format(order.getTotal())))),
                Dom.div("placed__footer",
                        new Button("Back to bookings", Button.Variant.DARK, () -> ctx.navigate("#/rider"))
                                .block(true).getElement()));
        Dom.clear(root);
        root.appendChild(page);
    }
}
