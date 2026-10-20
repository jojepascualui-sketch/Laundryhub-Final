package com.laundryhub.ui.pages;

import com.laundryhub.model.CashPayment;
import com.laundryhub.model.GCashPayment;
import com.laundryhub.model.LaundryLoad;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentMethod;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.model.WalkInService;
import com.laundryhub.service.PriceCalculator;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.BottomBar;
import com.laundryhub.ui.BottomNav;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Card;
import com.laundryhub.ui.Chip;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.IconButton;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.NavItem;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Responsive;
import com.laundryhub.ui.SegmentedControl;
import com.laundryhub.ui.SelectableCard;
import com.laundryhub.ui.Sidebar;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Stepper;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.ui.TopNav;
import com.laundryhub.ui.WizardHeader;
import com.laundryhub.util.MoneyFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * {@code #/dev/components}: every reusable component in its design states, for comparing
 * with docs/design/screens. Interactive parts work (selection, stepper, live total).
 */
public final class ComponentGalleryPage extends Page {

    /** Soap tile tints from the design (Standard, Hypoallergenic, Premium Scented, Baby-Safe). */
    private static final String[] SOAP_TINTS = {"#E4EEF8", "#E6F2EA", "#F3E8F7", "#FFF1E8"};
    private static final List<Soap> SOAPS = Arrays.asList(
            new Soap(1, "Standard", "Everyday clean for regular clothes", 0, true),
            new Soap(2, "Hypoallergenic", "Gentle on sensitive skin", 500, true),
            new Soap(3, "Premium Scented", "Long-lasting fresh fragrance", 800, true),
            new Soap(4, "Baby-Safe", "Mild and dye-free formula", 600, true));
    private static final int START_KG = 10;
    private static final int MAX_KG = 50;

    private final PriceCalculator calculator = new PriceCalculator(ShopSettings.defaults());
    private ServiceType service = PickupDeliveryService.INSTANCE;
    private Soap soap = SOAPS.get(1);
    private int kg = START_KG;
    private BottomBar bottomBar;
    private HTMLElement breakpointLabel;
    private Runnable stopListening;

    @Override
    public String getTitle() {
        return "Component gallery";
    }

    @Override
    protected HTMLElement render() {
        HTMLElement page = Dom.div("page gallery");
        page.appendChild(Dom.text("div", "page__kicker", "Developer"));
        page.appendChild(Dom.text("h1", "page__title", "Component gallery"));
        breakpointLabel = Dom.text("p", "page__lead", "");
        page.appendChild(breakpointLabel);
        updateBreakpointLabel();

        HTMLElement grid = Dom.div("gallery__grid");
        grid.appendChild(section("Buttons", buttons()));
        grid.appendChild(section("Segmented control", segmented()));
        grid.appendChild(section("Service cards (02)", serviceCards()));
        grid.appendChild(section("Weight stepper & soap tiles (03)", weightAndSoap()));
        grid.appendChild(section("Time slot chips (04)", slots()));
        grid.appendChild(section("Payment cards (05)", payments()));
        grid.appendChild(section("Text inputs", inputs()));
        grid.appendChild(section("Status badges", badges()));
        grid.appendChild(section("Cards (01 home)", homeCards()));
        grid.appendChild(section("Filter chips (A2)", filters()));
        grid.appendChild(section("Wizard header & bottom bar", wizard()));
        grid.appendChild(section("Empty, loading & toast", states()));
        grid.appendChild(section("Bottom nav (phone)", bottomNav()));
        grid.appendChild(section("Admin sidebar (A1)", sidebar()));
        page.appendChild(grid);

        HTMLElement topNav = section("Top nav (desktop)", new TopNav(customerNav(), "#/home", "Ana Reyes",
                () -> Toast.info("Sign out tapped")).getElement());
        topNav.getClassList().add("gallery__wide");
        page.appendChild(topNav);
        return page;
    }

    @Override
    protected void onMount() {
        stopListening = Responsive.addListener(b -> updateBreakpointLabel());
    }

    @Override
    protected void onUnmount() {
        if (stopListening != null) {
            stopListening.run();
        }
    }

    private void updateBreakpointLabel() {
        breakpointLabel.setTextContent("Current layout: " + Responsive.current().name().toLowerCase()
                + " (phone < " + Responsive.TABLET_MIN + " px ≤ tablet < " + Responsive.DESKTOP_MIN + " px ≤ desktop)");
    }

    private static HTMLElement section(String title, HTMLElement content) {
        HTMLElement s = Dom.el("section", "gallery__section");
        s.appendChild(Dom.text("h2", "gallery__heading", title));
        s.appendChild(content);
        return s;
    }

    private HTMLElement buttons() {
        Button disabled = new Button("Continue", Button.Variant.PRIMARY, null);
        disabled.setDisabled(true);
        Button loading = new Button("Place order", Button.Variant.PRIMARY, null);
        loading.setLoading(true);
        HTMLElement onBlue = Dom.div("gallery__on-accent", new Button("Start laundry order", Button.Variant.LIGHT,
                Icons.ARROW_RIGHT, true, () -> Toast.info("Start laundry order")).getElement());
        HTMLElement google = new Button("Continue with Google", Button.Variant.OUTLINE, Icons.GOOGLE, false,
                () -> Toast.info("Google sign-in")).block(true).getElement();
        return Dom.div("stack",
                Dom.div("row row--wrap",
                        new Button("Continue", Button.Variant.PRIMARY, () -> Toast.success("Continue tapped")).getElement(),
                        disabled.getElement(), loading.getElement()),
                Dom.div("row",
                        new Button("Decline", Button.Variant.OUTLINE, null).getElement(),
                        new Button("Accept booking", Button.Variant.PRIMARY, null).getElement()),
                new Button("Back to home", Button.Variant.DARK, null).block(true).getElement(),
                google,
                onBlue,
                Dom.div("row",
                        new Button("Sign up", Button.Variant.LINK, null).getElement(),
                        new IconButton(Icons.BACK, "Back", false, null).getElement(),
                        new IconButton(Icons.BELL, "Notifications", false, null).getElement(),
                        new IconButton(Icons.PHONE, "Call rider", true, null).getElement()));
    }

    private HTMLElement segmented() {
        return Dom.div("stack",
                new SegmentedControl(Arrays.asList("Customer", "Rider", "Admin"), 0, null).getElement(),
                new SegmentedControl(Arrays.asList("Available (4)", "My jobs (0)"), 0, null).getElement(),
                new SegmentedControl(Arrays.asList("Customers", "Riders", "Staff"), 0, null).compact(true).getElement());
    }

    private HTMLElement serviceCards() {
        HTMLElement box = Dom.div("stack");
        List<SelectableCard> cards = new ArrayList<>();
        for (ServiceType s : ServiceType.all()) {
            SelectableCard card = new SelectableCard(SelectableCard.Layout.ROW,
                    s == WalkInService.INSTANCE ? Icons.SHOP : Icons.TRUCK, null, null, null,
                    s.getDisplayName(), s.getDescription(), s.getFeeLabel(calculator.getSettings()), null);
            cards.add(card);
            box.appendChild(card.getElement());
        }
        for (int i = 0; i < cards.size(); i++) {
            final int index = i;
            cards.get(i).getElement().addEventListener("click", e -> {
                service = ServiceType.all().get(index);
                for (int k = 0; k < cards.size(); k++) {
                    cards.get(k).setSelected(k == index);
                }
                updateTotal();
            });
            cards.get(i).setSelected(ServiceType.all().get(i) == service);
        }
        return box;
    }

    private HTMLElement weightAndSoap() {
        HTMLElement weightCard = Dom.div("card stack",
                Dom.div("card__head", Dom.text("span", "card__title", "Laundry weight"),
                        Dom.text("span", "muted", MoneyFormatter.formatShort(calculator.getSettings().getRatePerKg()) + " per kg")),
                new Stepper(kg, LaundryLoad.MIN_KG, MAX_KG, "kilograms", value -> {
                    kg = value;
                    updateTotal();
                }).getElement(),
                Dom.div("note note--warn", Dom.icon(Icons.ALERT, null),
                        Dom.text("span", null, "Minimum of 8 kg per order. Below 8 kg is charged as 8 kg.")));

        HTMLElement soapGrid = Dom.div("grid-2");
        List<SelectableCard> tiles = new ArrayList<>();
        for (int i = 0; i < SOAPS.size(); i++) {
            Soap s = SOAPS.get(i);
            SelectableCard tile = new SelectableCard(SelectableCard.Layout.TILE, Icons.DROP, null,
                    SOAP_TINTS[i % SOAP_TINTS.length], "#14212B", s.getName(), s.getDescription(), s.getPriceLabel(), null);
            tiles.add(tile);
            soapGrid.appendChild(tile.getElement());
        }
        for (int i = 0; i < tiles.size(); i++) {
            final int index = i;
            tiles.get(i).getElement().addEventListener("click", e -> {
                soap = SOAPS.get(index);
                for (int k = 0; k < tiles.size(); k++) {
                    tiles.get(k).setSelected(k == index);
                }
                updateTotal();
            });
            tiles.get(i).setSelected(SOAPS.get(i) == soap);
        }
        return Dom.div("stack", weightCard, Dom.text("div", "section-title", "Fabric soap"), soapGrid);
    }

    private HTMLElement slots() {
        HTMLElement grid = Dom.div("grid-2");
        List<Chip> chips = new ArrayList<>();
        List<String> labels = PickupDeliveryService.INSTANCE.getTimeSlots();
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            Chip chip = new Chip(Chip.Style.SLOT, labels.get(i), () -> {
                for (int k = 0; k < chips.size(); k++) {
                    chips.get(k).setSelected(k == index);
                }
            });
            chips.add(chip);
            grid.appendChild(chip.getElement());
        }
        chips.get(0).setSelected(true);
        return grid;
    }

    private HTMLElement payments() {
        HTMLElement box = Dom.div("stack");
        List<PaymentMethod> methods = Arrays.asList(new CashPayment(), new GCashPayment(null, null));
        List<SelectableCard> cards = new ArrayList<>();
        String[] tints = {"#E6F2EA", "#E4EEF8"};
        String[] colors = {"#1D5E36", "#1E4FA8"};
        for (int i = 0; i < methods.size(); i++) {
            PaymentMethod m = methods.get(i);
            final int index = i;
            SelectableCard card = new SelectableCard(SelectableCard.Layout.ROW, null, m.getBadge(), tints[i], colors[i],
                    m.getDisplayName(), m.getDescription(service), null, () -> {
                        for (int k = 0; k < cards.size(); k++) {
                            cards.get(k).setSelected(k == index);
                        }
                    });
            cards.add(card);
            box.appendChild(card.getElement());
        }
        cards.get(0).setSelected(true);
        return box;
    }

    private HTMLElement inputs() {
        TextInput email = new TextInput("Email", "you@example.com", "email", null);
        TextInput mobile = new TextInput("Mobile number", "09XX XXX XXXX", "tel", "numeric");
        mobile.setValue("0917");
        mobile.setError("Enter your mobile number as 09XXXXXXXXX.");
        return Dom.div("stack", email.getElement(), mobile.getElement(),
                new TextInput("Address details", "House no., street, barangay, landmark", "text", null).getElement());
    }

    private HTMLElement badges() {
        HTMLElement row = Dom.div("row row--wrap");
        for (OrderStatus s : OrderStatus.values()) {
            row.appendChild(Badge.of(s).getElement());
        }
        HTMLElement kinds = Dom.div("row row--wrap",
                new Badge("Pickup", Badge.Tone.WASHING, true).getElement(),
                new Badge("Return delivery", Badge.Tone.OUT, true).getElement(),
                new Badge("Active", Badge.Tone.DONE, false).getElement(),
                new Badge("Out for delivery", Badge.Tone.OUT, true).getElement());
        return Dom.div("stack", row, kinds);
    }

    private HTMLElement homeCards() {
        HTMLElement rate = Dom.div("tile",
                Dom.text("span", "tile__label", "Wash · Dry · Fold"),
                Dom.div("tile__value", Dom.text("span", null, "₱30"), Dom.text("span", "tile__unit", " /kg")));
        HTMLElement min = Dom.div("tile tile--warn",
                Dom.text("span", "tile__label", "Minimum load"),
                Dom.text("span", "tile__value", "8 kg"));
        HTMLElement summary = new Card(null,
                Card.title("Order summary"),
                summaryRow("Service", "Pickup & Delivery"),
                summaryRow("Laundry (10 kg × ₱30)", "₱300.00"),
                summaryRow("Hypoallergenic (10 kg × ₱5)", "₱50.00"),
                summaryRow("Pickup & delivery fee", "₱60.00"),
                Dom.el("div", "divider"),
                Dom.div("summary-total", Dom.text("span", "summary-total__label", "Total"),
                        Dom.text("span", "summary-total__value", "₱410.00"))).getElement();
        return Dom.div("stack", Dom.div("grid-2", rate, min), summary);
    }

    private static HTMLElement summaryRow(String label, String value) {
        return Dom.div("summary-row", Dom.text("span", "summary-row__label", label), Dom.text("span", "summary-row__value", value));
    }

    private HTMLElement filters() {
        String[] labels = {"All (6)", "Pending (2)", "Washing (1)", "Ready (1)", "Out for delivery (1)", "Completed (1)"};
        HTMLElement row = Dom.div("row row--wrap");
        List<Chip> chips = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Chip chip = new Chip(Chip.Style.FILTER, labels[i], () -> {
                for (int k = 0; k < chips.size(); k++) {
                    chips.get(k).setSelected(k == index);
                }
            });
            chips.add(chip);
            row.appendChild(chip.getElement());
        }
        chips.get(0).setSelected(true);
        return row;
    }

    private HTMLElement wizard() {
        bottomBar = new BottomBar("Continue", 0, () -> Toast.info("Continue"));
        updateTotal();
        HTMLElement frame = Dom.div("gallery__frame",
                new WizardHeader(2, 4, "Weight & fabric soap", () -> Toast.info("Back")).getElement(),
                Dom.div("gallery__frame-body", Dom.text("p", "muted", "Change service, kilos or soap above: the total updates live.")),
                bottomBar.getElement());
        return frame;
    }

    private void updateTotal() {
        if (bottomBar != null) {
            bottomBar.setTotal(calculator.quote(service, new LaundryLoad(kg, soap)).getTotal());
        }
    }

    private HTMLElement states() {
        return Dom.div("stack",
                new EmptyState("You’re off duty", "Go on duty to receive new pickup and delivery bookings.", null).getElement(),
                new Spinner("Loading orders…").getElement(),
                Dom.div("row row--wrap",
                        new Button("Show info toast", Button.Variant.OUTLINE, () -> Toast.info("Saved as draft.")).getElement(),
                        new Button("Show success toast", Button.Variant.OUTLINE, () -> Toast.success("Order placed!")).getElement(),
                        new Button("Show error toast", Button.Variant.OUTLINE,
                                () -> Toast.error("This booking was already taken.")).getElement()));
    }

    private static List<NavItem> customerNav() {
        return Arrays.asList(new NavItem("Home", "#/home", Icons.HOME), new NavItem("Orders", "#/orders", Icons.ORDERS),
                new NavItem("Profile", "#/profile", Icons.PROFILE));
    }

    private HTMLElement bottomNav() {
        HTMLElement frame = Dom.div("gallery__frame gallery__frame--nav");
        frame.appendChild(new BottomNav(customerNav(), "#/home").getElement());
        return frame;
    }

    private HTMLElement sidebar() {
        List<NavItem> items = Arrays.asList(new NavItem("Dashboard", "#/admin", null),
                new NavItem("Orders", "#/admin/orders", null), new NavItem("Pricing & soaps", "#/admin/pricing", null),
                new NavItem("Customers & riders", "#/admin/users", null), new NavItem("Sales reports", "#/admin/reports", null));
        HTMLElement frame = Dom.div("gallery__frame gallery__frame--sidebar");
        frame.appendChild(new Sidebar(items, "#/admin", "Admin", "Owner", () -> Toast.info("Log out")).getElement());
        return frame;
    }
}
