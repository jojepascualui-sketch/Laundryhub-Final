package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Booking;
import com.laundryhub.model.Location;
import com.laundryhub.model.Rider;
import com.laundryhub.model.RiderJob;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.service.RiderBoard;
import com.laundryhub.ui.BookingCard;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.InstallCard;
import com.laundryhub.ui.JobMap;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.RiderLocationSharer;
import com.laundryhub.ui.SegmentedControl;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.web.AppContext;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screens R1 / R5: the rider's bookings. Dark header with the On duty switch and counters,
 * Available / My jobs tabs, booking cards (accept / decline), off-duty and empty states.
 * Refreshes every 15 s; while on duty the GPS is shared every 10 s.
 * Desktop: bookings on the left, the active job's map on the right.
 */
public final class RiderHomePage extends Page {

    private static final int REFRESH_MILLIS = 15000;
    private static final int TAB_AVAILABLE = 0;
    private static final int TAB_MINE = 1;

    private final AppContext ctx;
    private int tab = TAB_AVAILABLE;
    private RiderBoard board;
    private ShopSettings settings;
    private int timer = -1;
    private Runnable stopGps;

    private HTMLElement dutyButton;
    private HTMLElement availableCount;
    private HTMLElement acceptedCount;
    private HTMLElement doneCount;
    private SegmentedControl tabs;
    private HTMLElement list;
    private HTMLElement side;
    private long sideJobId = Long.MIN_VALUE;
    private int sideStep = -1;
    private JobMap sideMap;

    /**
     * @param ctx app context
     */
    public RiderHomePage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Bookings";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.RIDER);
    }

    private Rider rider() {
        return (Rider) ctx.getCurrentUser();
    }

    @Override
    protected HTMLElement render() {
        Rider rider = rider();
        dutyButton = Dom.el("button", "duty");
        dutyButton.setAttribute("type", "button");
        Dom.onClick(dutyButton, this::toggleDuty);

        HTMLElement signOut = Dom.text("button", "rider-header__signout", "Sign out");
        signOut.setAttribute("type", "button");
        Dom.onClick(signOut, () -> {
            RiderLocationSharer.stop();
            ctx.signOut();
        });

        availableCount = Dom.text("div", "counter__value", "0");
        acceptedCount = Dom.text("div", "counter__value", "0");
        doneCount = Dom.text("div", "counter__value", "0");
        HTMLElement header = Dom.el("header", "rider-header");
        Dom.append(header,
                Dom.div("rider-header__row",
                        Dom.div("rider-header__who",
                                Dom.div("rider-header__top", Dom.text("span", "rider-header__app", "Laundry HUB Rider"), signOut),
                                Dom.text("h1", "rider-header__hi", "Hi, " + rider.getFirstName())),
                        dutyButton),
                Dom.div("counters",
                        Dom.div("counter", Dom.text("div", "counter__label", "Available"), availableCount),
                        Dom.div("counter", Dom.text("div", "counter__label", "Accepted"), acceptedCount),
                        Dom.div("counter", Dom.text("div", "counter__label", "Done today"), doneCount)));

        tabs = new SegmentedControl(Arrays.asList("Available (0)", "My jobs (0)"), tab, i -> {
            tab = i;
            drawList();
        });
        list = Dom.div("stack rider-list");
        list.appendChild(new Spinner("Loading bookings…").getElement());
        side = Dom.el("aside", "rider-side");

        HTMLElement main = Dom.div("rider-main", Dom.div("rider-tabs", tabs.getElement()),
                Dom.div("rider-install", new InstallCard().getElement()), list);
        HTMLElement body = Dom.div("rider-body", main, side);
        drawDuty();
        return Dom.div("rider-page", header, body);
    }

    @Override
    protected void onMount() {
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog catalog) {
                settings = catalog.getSettings();
            }

            @Override
            public void onError(ApiException error) {
                // the shop pin is optional on this screen
            }
        });
        load();
        timer = Window.setInterval(this::load, REFRESH_MILLIS);
        if (rider().isOnDuty()) {
            RiderLocationSharer.start(ctx.getRiderJobs());
        }
        stopGps = RiderLocationSharer.addListener(p -> {
            drawList();
            if (sideMap != null) {
                sideMap.setRider(p);
            }
        });
    }

    @Override
    protected void onUnmount() {
        if (timer >= 0) {
            Window.clearInterval(timer);
        }
        if (stopGps != null) {
            stopGps.run();
        }
        // Keep sharing only while a rider page is open.
        if (!Window.current().getLocation().getHash().startsWith("#/rider")) {
            RiderLocationSharer.stop();
        }
    }

    // ---- data ---------------------------------------------------------------------

    private void load() {
        ctx.getRiderJobs().findMine(new Callback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> mine) {
                if (!rider().isOnDuty()) {
                    board = new RiderBoard(null, mine, System.currentTimeMillis());
                    drawAll();
                    return;
                }
                ctx.getRiderJobs().findAvailable(new Callback<List<Booking>>() {
                    @Override
                    public void onSuccess(List<Booking> available) {
                        board = new RiderBoard(available, mine, System.currentTimeMillis());
                        drawAll();
                    }

                    @Override
                    public void onError(ApiException error) {
                        showLoadError(error);
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                showLoadError(error);
            }
        });
    }

    private void showLoadError(ApiException error) {
        if (board == null) {
            Dom.clear(list);
            list.appendChild(new EmptyState("Couldn't load bookings", error.getMessage(),
                    new Button("Try again", Button.Variant.PRIMARY, this::load)).getElement());
        }
    }

    // ---- drawing --------------------------------------------------------------------

    private void drawAll() {
        availableCount.setTextContent(Integer.toString(board.getAvailableCount()));
        acceptedCount.setTextContent(Integer.toString(board.getAcceptedCount()));
        doneCount.setTextContent(Integer.toString(board.getDoneTodayCount()));
        tabs.setLabel(TAB_AVAILABLE, "Available (" + board.getAvailableCount() + ")");
        tabs.setLabel(TAB_MINE, "My jobs (" + board.getMyJobs().size() + ")");
        drawList();
        drawSide();
    }

    private void drawDuty() {
        boolean on = rider().isOnDuty();
        Dom.clear(dutyButton);
        dutyButton.setClassName(on ? "duty duty--on" : "duty");
        dutyButton.setAttribute("aria-pressed", on ? "true" : "false");
        dutyButton.appendChild(Dom.text("span", null, on ? "On duty" : "Off duty"));
        dutyButton.appendChild(Dom.div("duty__track", Dom.el("span", "duty__knob")));
    }

    private void drawList() {
        if (board == null) {
            return;
        }
        Dom.clear(list);
        Location me = RiderLocationSharer.getLatest();
        if (tab == TAB_AVAILABLE) {
            if (!rider().isOnDuty()) {
                list.appendChild(new EmptyState("You’re off duty",
                        "Go on duty to receive new pickup and delivery bookings.", null).getElement());
                return;
            }
            if (board.getAvailable().isEmpty()) {
                list.appendChild(new EmptyState(null, "No new bookings right now. New ones will appear here.", null).getElement());
                return;
            }
            for (Booking b : board.getAvailable()) {
                BookingCard[] card = new BookingCard[1];
                card[0] = new BookingCard(b, BookingCard.Mode.AVAILABLE, me,
                        () -> accept(b, card[0]), () -> decline(b));
                list.appendChild(card[0].getElement());
            }
        } else {
            List<Booking> mine = board.getMyJobs();
            if (mine.isEmpty()) {
                list.appendChild(new EmptyState(null, "You haven’t accepted any bookings yet.", null).getElement());
                return;
            }
            for (Booking b : mine) {
                list.appendChild(new BookingCard(b, BookingCard.Mode.MINE, me,
                        () -> ctx.navigate("#/rider/jobs/" + b.getJob().getId()), null).getElement());
            }
        }
    }

    /** Desktop only: the active job next to the list. */
    private void drawSide() {
        Booking active = board.getActiveJob();
        long activeId = active == null ? -1 : active.getJob().getId();
        int step = active == null ? -1 : active.getJob().getCurrentStep();
        if (activeId == sideJobId && step == sideStep) {
            return; // unchanged: keep the map as it is
        }
        sideJobId = activeId;
        sideStep = step;
        sideMap = null;
        Dom.clear(side);
        if (active == null) {
            side.appendChild(new EmptyState("No active job", "Accept a booking and it will show here with its map.", null).getElement());
            return;
        }
        RiderJob job = active.getJob();
        Location shop = settings == null ? null : settings.getShopLocation();
        sideMap = new JobMap(shop, active.getOrder().getLocation(), RiderLocationSharer.getLatest(),
                RiderBoard.destination(active, shop));
        HTMLElement info = Dom.div("card stack--tight",
                Dom.div("booking__top", BookingCard.typeBadge(job).getElement(),
                        Dom.text("span", "booking__meta", active.getOrder().getOrderNo())),
                Dom.text("div", "booking__name", active.getOrder().getCustomerName()),
                Dom.text("div", "booking__line", BookingCard.addressLine(active.getOrder())),
                Dom.text("div", "booking__line", "Next: " + job.getNextActionLabel() + " · "
                        + MoneyFormatter.format(active.getOrder().getTotal())),
                new Button("Continue job", Button.Variant.PRIMARY,
                        () -> ctx.navigate("#/rider/jobs/" + job.getId())).block(true).getElement());
        Dom.append(side, sideMap.getElement(), info);
    }

    // ---- actions --------------------------------------------------------------------

    private void toggleDuty() {
        boolean next = !rider().isOnDuty();
        dutyButton.setAttribute("disabled", "");
        ctx.getRiderJobs().setOnDuty(next, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dutyButton.removeAttribute("disabled");
                ctx.setCurrentUser(rider().withOnDuty(next));
                drawDuty();
                if (next) {
                    RiderLocationSharer.start(ctx.getRiderJobs());
                    Toast.success("You're on duty. New bookings will appear here.");
                } else {
                    RiderLocationSharer.stop();
                }
                load();
            }

            @Override
            public void onError(ApiException error) {
                dutyButton.removeAttribute("disabled");
                Toast.error(error.getMessage());
            }
        });
    }

    private void accept(Booking booking, BookingCard card) {
        card.getPrimaryButton().setLoading(true);
        ctx.getRiderJobs().accept(booking.getJob().getId(), new Callback<RiderJob>() {
            @Override
            public void onSuccess(RiderJob job) {
                ctx.navigate("#/rider/jobs/" + job.getId());
            }

            @Override
            public void onError(ApiException error) {
                card.getPrimaryButton().setLoading(false);
                Toast.error(error.isConflict() ? "This booking was already taken." : error.getMessage());
                load();
            }
        });
    }

    private void decline(Booking booking) {
        ctx.getRiderJobs().decline(booking.getJob().getId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.info("Booking hidden. Other riders can still take it.");
                load();
            }

            @Override
            public void onError(ApiException error) {
                Toast.error(error.getMessage());
                load();
            }
        });
    }
}
