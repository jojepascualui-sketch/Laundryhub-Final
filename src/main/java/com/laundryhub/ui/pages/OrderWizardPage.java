package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.CashPayment;
import com.laundryhub.model.GCashPayment;
import com.laundryhub.model.LaundryLoad;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderRequest;
import com.laundryhub.model.PickupDeliveryService;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.Role;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.model.WalkInService;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.service.OrderDraft;
import com.laundryhub.service.PriceCalculator;
import com.laundryhub.ui.BottomBar;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Chip;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.LocationPicker;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.SelectableCard;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Stepper;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.ui.WizardHeader;
import com.laundryhub.util.Callback;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TextUtils;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Screens 02–05: the 4-step order wizard ({@code #/order/1} … {@code #/order/4}).
 * Live "Estimated total" from {@link PriceCalculator}; the review step shows the server's price
 * (rpc quote_order) and "Place order" calls rpc place_order.
 * Desktop: two columns with a sticky Order summary card on the right.
 */
public final class OrderWizardPage extends Page {

    /** Soap tile tints from the design, used in order. */
    private static final String[] SOAP_TINTS = {"#E4EEF8", "#E6F2EA", "#F3E8F7", "#FFF1E8"};
    private static final int MAX_KG = 100;
    /** Shown size of the QR image in px. */
    private static final int QR_SIZE = 220;
    /** How to pay with the QR Ph code. */
    private static final String[] GCASH_QR_STEPS = {
        "Open GCash → Pay QR and scan the code (on this phone: Save QR, then Upload QR).",
        "Or use Send Money to our GCash number.",
        "Pay the exact amount, then enter the 13-digit reference number below."};
    /** How to pay with GCash Send Money (the shop checks the reference number). */
    private static final String[] GCASH_STEPS = {
        "Open GCash → Send Money → Express Send.",
        "Paste our number and send the exact amount.",
        "Copy the 13-digit reference number from the receipt and enter it below."};

    private final AppContext ctx;
    private final int step;
    private final OrderDraft draft;
    private CatalogCache.Catalog catalog;
    private PriceCalculator calculator;
    private PriceBreakdown serverQuote;

    private BottomBar bottomBar;
    private Button asideButton;
    private HTMLElement asideLines;
    private HTMLElement notice;
    private HTMLElement content;
    private HTMLInputElement proofInput;
    private final List<Runnable> refreshers = new ArrayList<>();

    /**
     * @param ctx  app context
     * @param step 1..4 (from the route)
     */
    public OrderWizardPage(AppContext ctx, int step) {
        this.ctx = ctx;
        this.step = Math.max(OrderDraft.STEP_SERVICE, Math.min(OrderDraft.TOTAL_STEPS, step));
        this.draft = ctx.getOrderDraft();
    }

    @Override
    public String getTitle() {
        return draft.stepTitle(step);
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected HTMLElement render() {
        if (catalog == null && ctx.getCatalogCache().getLoaded() != null) {
            useCatalog(ctx.getCatalogCache().getLoaded());
        }
        if (catalog == null) {
            HTMLElement loading = Dom.div("page");
            loading.appendChild(new Spinner("Loading prices…").getElement());
            loadCatalog();
            return shell(loading);
        }
        int firstOpen = draft.firstIncompleteStep(catalog.getSettings());
        if (step > firstOpen) {
            // Opened a later step directly (refresh / typed URL): go back to the first missing step.
            ctx.navigate("#/order/" + firstOpen);
            return shell(Dom.div("page"));
        }

        refreshers.clear();
        notice = Dom.el("div", "notice notice--error");
        notice.setAttribute("role", "alert");
        notice.setHidden(true);

        content = Dom.div("wizard__content");
        content.appendChild(notice);
        switch (step) {
            case OrderDraft.STEP_SERVICE:
                buildServiceStep(content);
                break;
            case OrderDraft.STEP_LOAD:
                buildLoadStep(content);
                break;
            case OrderDraft.STEP_SCHEDULE:
                if (draft.getService() != null && draft.getService().requiresLocation()) {
                    buildDeliveryStep(content);
                } else {
                    buildWalkInStep(content);
                }
                break;
            default:
                buildReviewStep(content);
        }

        String label = step == OrderDraft.STEP_REVIEW ? "Place order" : "Continue";
        bottomBar = new BottomBar(label, 0, this::next);
        asideButton = new Button(label, Button.Variant.PRIMARY, this::next).block(true);
        asideLines = Dom.div("stack");
        HTMLElement aside = Dom.el("aside", "wizard__aside");
        aside.appendChild(Dom.div("card wizard__summary", Dom.text("h2", "card__title", "Order summary"), asideLines,
                asideButton.getElement()));

        HTMLElement wizard = Dom.div("wizard",
                new WizardHeader(step, OrderDraft.TOTAL_STEPS, draft.stepTitle(step), this::back).getElement(),
                Dom.div("wizard__body", content, aside),
                bottomBar.getElement());
        update();
        return shell(wizard);
    }

    private HTMLElement shell(HTMLElement inner) {
        return new CustomerShell("#/home", ctx.getCurrentUser() == null ? null : ctx.getCurrentUser().getDisplayName(),
                ctx::signOut, inner, false).getElement();
    }

    private void loadCatalog() {
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog result) {
                useCatalog(result);
                rerender();
            }

            @Override
            public void onError(ApiException error) {
                Dom.clear(getElement());
                Button retry = new Button("Try again", Button.Variant.PRIMARY, () -> {
                    ctx.getCatalogCache().invalidate();
                    rerender();
                });
                getElement().appendChild(Dom.div("page page--narrow",
                        new EmptyState("Couldn't load prices", error.getMessage(), retry).getElement()));
            }
        });
    }

    private void useCatalog(CatalogCache.Catalog result) {
        catalog = result;
        calculator = new PriceCalculator(result.getSettings());
        if (draft.getSoap() == null && !result.getSoaps().isEmpty()) {
            draft.setSoap(result.getSoaps().get(0));
        }
        if (draft.getKg() < result.getSettings().getMinKg()) {
            draft.setKg(result.getSettings().getMinKg());
        }
    }

    // ---- step 1: service (02) ----------------------------------------------------

    private void buildServiceStep(HTMLElement box) {
        box.appendChild(Dom.text("p", "wizard__hint", "How do you want to hand over your laundry?"));
        List<SelectableCard> cards = new ArrayList<>();
        for (ServiceType service : ServiceType.all()) {
            SelectableCard card = new SelectableCard(SelectableCard.Layout.ROW,
                    service == WalkInService.INSTANCE ? Icons.SHOP : Icons.TRUCK, null, null, null,
                    service.getDisplayName(), service.getDescription(),
                    service.getFeeLabel(catalog.getSettings()), null);
            cards.add(card);
            box.appendChild(card.getElement());
        }
        for (int i = 0; i < cards.size(); i++) {
            ServiceType service = ServiceType.all().get(i);
            SelectableCard card = cards.get(i);
            card.setSelected(service == draft.getService());
            card.getElement().addEventListener("click", e -> {
                draft.setService(service);
                for (int k = 0; k < cards.size(); k++) {
                    cards.get(k).setSelected(ServiceType.all().get(k) == service);
                }
                update();
            });
        }
    }

    // ---- step 2: weight & soap (03) ------------------------------------------------

    private void buildLoadStep(HTMLElement box) {
        ShopSettings s = catalog.getSettings();
        int min = s.getMinKg();
        HTMLElement weight = Dom.div("card",
                Dom.div("card__head", Dom.text("span", "card__title", "Laundry weight"),
                        Dom.text("span", "muted", MoneyFormatter.formatShort(s.getRatePerKg()) + " per kg")),
                new Stepper(Math.max(draft.getKg(), min), min, MAX_KG, "kilograms", value -> {
                    draft.setKg(value);
                    update();
                }).getElement(),
                Dom.div("note note--warn", Dom.icon(Icons.ALERT, null),
                        Dom.text("span", null, "Minimum of " + min + " kg per order. Below " + min
                                + " kg is charged as " + min + " kg.")));
        box.appendChild(weight);

        box.appendChild(Dom.text("h2", "section-title", "Fabric soap"));
        if (catalog.getSoaps().isEmpty()) {
            box.appendChild(new EmptyState(null, "No fabric soaps are available right now. Please try again later.",
                    null).getElement());
            return;
        }
        HTMLElement grid = Dom.div("grid-2");
        List<SelectableCard> tiles = new ArrayList<>();
        List<Soap> soaps = catalog.getSoaps();
        for (int i = 0; i < soaps.size(); i++) {
            Soap soap = soaps.get(i);
            SelectableCard tile = new SelectableCard(SelectableCard.Layout.TILE, Icons.DROP, null,
                    SOAP_TINTS[i % SOAP_TINTS.length], "#14212B", soap.getName(), soap.getDescription(),
                    soap.getPriceLabel(), null);
            tiles.add(tile);
            grid.appendChild(tile.getElement());
        }
        for (int i = 0; i < soaps.size(); i++) {
            Soap soap = soaps.get(i);
            tiles.get(i).setSelected(draft.getSoap() != null && draft.getSoap().getId() == soap.getId());
            tiles.get(i).getElement().addEventListener("click", e -> {
                draft.setSoap(soap);
                for (int k = 0; k < soaps.size(); k++) {
                    tiles.get(k).setSelected(soaps.get(k) == soap);
                }
                update();
            });
        }
        box.appendChild(grid);
    }

    // ---- step 3: walk-in schedule (04b) ----------------------------------------------

    private void buildWalkInStep(HTMLElement box) {
        ShopSettings s = catalog.getSettings();
        HTMLElement details = Dom.div("info-card__text");
        details.appendChild(Dom.text("span", "info-card__title", "Drop off at Laundry HUB"));
        details.appendChild(Dom.text("span", "info-card__line", s.getShopAddress().isEmpty() ? "[SHOP ADDRESS]" : s.getShopAddress()));
        details.appendChild(Dom.text("span", "info-card__line", "Open " + (s.getShopHours().isEmpty() ? "[SHOP HOURS]" : s.getShopHours())));
        box.appendChild(Dom.div("card info-card", Dom.icon(Icons.PIN, "info-card__icon"), details));
        box.appendChild(slotChips(WalkInService.INSTANCE));
    }

    // ---- step 3: delivery location (04a) --------------------------------------------

    private void buildDeliveryStep(HTMLElement box) {
        box.appendChild(Dom.text("p", "wizard__hint",
                "Move the map so the pin is where our rider should pick up and return your laundry."));
        box.appendChild(new LocationPicker(catalog.getSettings(), draft.getPin(), location -> {
            draft.setPin(location);
            update();
        }).getElement());

        TextInput address = new TextInput("Address details", "House no., street, barangay, landmark", "text", null)
                .onInput(v -> draft.setAddressDetails(v));
        address.setValue(draft.getAddressDetails());
        box.appendChild(address.getElement());
        box.appendChild(slotChips(PickupDeliveryService.INSTANCE));
    }

    private HTMLElement slotChips(ServiceType service) {
        HTMLElement box = Dom.div("stack stack--tight");
        box.appendChild(Dom.text("h2", "section-title", service.getSlotTitle()));
        HTMLElement grid = Dom.div("grid-2");
        List<Chip> chips = new ArrayList<>();
        for (String label : service.getTimeSlots()) {
            Chip chip = new Chip(Chip.Style.SLOT, label, null);
            chips.add(chip);
            grid.appendChild(chip.getElement());
        }
        for (Chip chip : chips) {
            chip.setSelected(chip.getLabel().equals(draft.getSlot()));
            chip.getElement().addEventListener("click", e -> {
                draft.setSlot(chip.getLabel());
                for (Chip c : chips) {
                    c.setSelected(c == chip);
                }
                update();
            });
        }
        box.appendChild(grid);
        return box;
    }

    // ---- step 4: review & pay (05) -------------------------------------------------

    private void buildReviewStep(HTMLElement box) {
        ShopSettings s = catalog.getSettings();
        HTMLElement summaryLines = Dom.div("stack stack--tight");
        HTMLElement totalValue = Dom.text("span", "summary-total__value", "");
        HTMLElement summary = Dom.div("card wizard__review-summary", Dom.text("h2", "card__title", "Order summary"),
                summaryLines, Dom.el("div", "divider"),
                Dom.div("summary-total", Dom.text("span", "summary-total__label", "Total"), totalValue));
        box.appendChild(summary);
        refreshers.add(() -> {
            PriceBreakdown p = currentBreakdown();
            fillSummaryLines(summaryLines, p);
            totalValue.setTextContent(MoneyFormatter.format(p.getTotal()));
        });
        requestServerQuote();

        box.appendChild(Dom.text("h2", "section-title", "Payment method"));
        if (!s.isCashEnabled() && CashPayment.CODE.equals(draft.getPaymentCode())) {
            draft.setPaymentCode(GCashPayment.CODE);
        }
        if (!s.isGcashEnabled() && draft.isGcash()) {
            draft.setPaymentCode(CashPayment.CODE);
        }
        CashPayment cash = new CashPayment();
        GCashPayment gcash = new GCashPayment(null, null);
        SelectableCard cashCard = new SelectableCard(SelectableCard.Layout.ROW, null, cash.getBadge(), "#E6F2EA", "#1D5E36",
                cash.getDisplayName(), cash.getDescription(draft.getService()), null, null);
        SelectableCard gcashCard = new SelectableCard(SelectableCard.Layout.ROW, null, gcash.getBadge(), "#E4EEF8", "#1E4FA8",
                gcash.getDisplayName(), gcash.getDescription(draft.getService()), null, null);
        cashCard.setDisabled(!s.isCashEnabled());
        gcashCard.setDisabled(!s.isGcashEnabled());
        box.appendChild(Dom.div("stack stack--tight", cashCard.getElement(), gcashCard.getElement()));

        HTMLElement gcashPanel = buildGcashPanel(s);
        box.appendChild(gcashPanel);

        Runnable refreshPayment = () -> {
            cashCard.setSelected(!draft.isGcash());
            gcashCard.setSelected(draft.isGcash());
            gcashPanel.setHidden(!draft.isGcash());
        };
        refreshers.add(refreshPayment);
        cashCard.getElement().addEventListener("click", e -> {
            if (s.isCashEnabled()) {
                draft.setPaymentCode(CashPayment.CODE);
                update();
            }
        });
        gcashCard.getElement().addEventListener("click", e -> {
            if (s.isGcashEnabled()) {
                draft.setPaymentCode(GCashPayment.CODE);
                update();
            }
        });
    }

    private HTMLElement buildGcashPanel(ShopSettings s) {
        HTMLElement panel = Dom.div("stack");
        HTMLElement sendTo = Dom.div("card gcash-card");
        HTMLElement amount = Dom.text("strong", null, "");
        sendTo.appendChild(Dom.div("gcash-card__head", Dom.text("span", null, "Send "), amount,
                Dom.text("span", null, " to our GCash, then enter the reference number below.")));
        if (Validators.hasText(s.getGcashNumber())) {
            if (s.getGcashQrPath() != null) {
                sendTo.appendChild(qrBlock(ctx.getCatalog().gcashQrUrl(s.getGcashQrPath())));
            }
            String shopNumber = Validators.compact(s.getGcashNumber());
            HTMLElement copy = Dom.text("button", "gcash-card__copy", "Copy");
            copy.setAttribute("type", "button");
            copy.setAttribute("aria-label", "Copy GCash number");
            Dom.onClick(copy, () -> {
                Browser.copyText(shopNumber);
                Toast.success("GCash number copied. Paste it in GCash → Send Money.");
            });
            sendTo.appendChild(Dom.div("summary-row", Dom.text("span", "summary-row__label", "GCash number"),
                    Dom.div("gcash-card__number", Dom.text("span", "summary-row__value", TextUtils.formatMobile(shopNumber)), copy)));
            if (Validators.hasText(s.getGcashAccountName())) {
                sendTo.appendChild(summaryRow("Account name", s.getGcashAccountName()));
            }
            HTMLElement steps = Dom.el("ol", "gcash-card__steps");
            for (String step : s.getGcashQrPath() != null ? GCASH_QR_STEPS : GCASH_STEPS) {
                steps.appendChild(Dom.text("li", null, step));
            }
            sendTo.appendChild(steps);
        } else {
            sendTo.appendChild(Dom.text("div", "note note--warn",
                    "The shop has not added its GCash number yet. Please choose Cash or ask the shop."));
        }
        refreshers.add(() -> amount.setTextContent(MoneyFormatter.format(currentBreakdown().getTotal())));
        panel.appendChild(sendTo);

        TextInput number = new TextInput("GCash mobile number", "09XX XXX XXXX", "tel", "numeric")
                .onInput(v -> {
                    draft.setGcashNumber(v);
                    update();
                });
        number.setValue(draft.getGcashNumber());
        TextInput reference = new TextInput("GCash reference number", "13-digit reference no.", "text", "numeric")
                .onInput(v -> {
                    draft.setGcashReference(v);
                    update();
                });
        reference.setValue(draft.getGcashReference());
        refreshers.add(() -> {
            String n = draft.getGcashNumber();
            number.setError(Validators.hasText(n) && Validators.compact(n).length() >= 11 && !Validators.isMobile(n)
                    ? "Enter your GCash number as 09XXXXXXXXX." : null);
            String r = Validators.compact(draft.getGcashReference());
            reference.setError(r.length() > Validators.GCASH_REFERENCE_LENGTH
                    || (r.length() == Validators.GCASH_REFERENCE_LENGTH && !Validators.isGcashReference(r))
                    ? "The reference number has 13 digits." : null);
        });

        proofInput = Dom.input("file", "file-input");
        proofInput.setAttribute("accept", "image/*");
        proofInput.setId("gcash-proof");
        HTMLElement proofLabel = Dom.text("label", "field__label", "Payment screenshot (optional)");
        proofLabel.setAttribute("for", "gcash-proof");
        HTMLElement proof = Dom.div("field", proofLabel, proofInput,
                Dom.text("span", "muted", "Helps the shop verify your payment faster."));

        Dom.append(panel, number.getElement(), reference.getElement(), proof);
        return panel;
    }

    /** The shop's QR Ph code with a "Save QR" button (to upload it in GCash on the same phone). */
    private static HTMLElement qrBlock(String url) {
        HTMLElement img = Dom.el("img", "gcash-qr__img");
        img.setAttribute("src", url);
        img.setAttribute("alt", "GCash QR Ph code of The Laundry Hub Iloilo");
        img.setAttribute("width", String.valueOf(QR_SIZE));
        img.setAttribute("height", String.valueOf(QR_SIZE));
        img.setAttribute("loading", "lazy");
        Button saveQr = new Button("Save QR", Button.Variant.OUTLINE, () -> {
            Browser.saveFile(url, "laundryhub-gcash-qr.png");
            Toast.success("QR saved. In GCash tap Pay QR → Upload QR.");
        });
        saveQr.getElement().getClassList().add("btn--small");
        return Dom.div("gcash-qr", Dom.text("span", "gcash-qr__badge", "QR Ph"), img,
                Dom.text("span", "gcash-qr__caption", "Scan with GCash or any QR Ph bank app"), saveQr.getElement());
    }

    private static HTMLElement summaryRow(String label, String value) {
        return Dom.div("summary-row", Dom.text("span", "summary-row__label", label),
                Dom.text("span", "summary-row__value", value));
    }

    private void fillSummaryLines(HTMLElement lines, PriceBreakdown p) {
        Dom.clear(lines);
        if (draft.getService() != null) {
            lines.appendChild(summaryRow("Service", draft.getService().getDisplayName()));
        }
        lines.appendChild(summaryRow("Laundry (" + p.getChargedKg() + " kg × " + MoneyFormatter.formatShort(p.getRatePerKg()) + ")",
                MoneyFormatter.format(p.getLaundryCost())));
        if (draft.getSoap() != null) {
            lines.appendChild(summaryRow(draft.getSoap().getName() + " (" + p.getChargedKg() + " kg × "
                    + MoneyFormatter.formatShort(p.getSoapAddOnPerKg()) + ")", MoneyFormatter.format(p.getSoapCost())));
        }
        if (draft.getService() != null && draft.getService().requiresLocation()) {
            lines.appendChild(summaryRow("Pickup & delivery fee", MoneyFormatter.format(p.getDeliveryFee())));
        }
    }

    /** @return the server's quote on the review step, otherwise the local estimate */
    private PriceBreakdown currentBreakdown() {
        if (step == OrderDraft.STEP_REVIEW && serverQuote != null) {
            return serverQuote;
        }
        Soap soap = draft.getSoap() != null ? draft.getSoap() : catalog.getSoaps().isEmpty() ? null : catalog.getSoaps().get(0);
        ServiceType service = draft.getService() != null ? draft.getService() : WalkInService.INSTANCE;
        if (soap == null) {
            int kg = Math.max(draft.getKg(), catalog.getSettings().getMinKg());
            long rate = catalog.getSettings().getRatePerKg();
            return new PriceBreakdown(kg, rate, 0, kg * rate, 0, service.getServiceFee(catalog.getSettings()));
        }
        PriceBreakdown p = calculator.quote(service, new LaundryLoad(draft.getKg(), soap));
        if (draft.getService() == null) {
            // No service yet: no delivery fee in the estimate.
            return new PriceBreakdown(p.getChargedKg(), p.getRatePerKg(), p.getSoapAddOnPerKg(),
                    p.getLaundryCost(), p.getSoapCost(), 0);
        }
        return p;
    }

    private void requestServerQuote() {
        if (draft.getService() == null || draft.getSoap() == null) {
            return;
        }
        ctx.getOrders().quote(draft.getService(), draft.getKg(), draft.getSoap().getId(), new Callback<PriceBreakdown>() {
            @Override
            public void onSuccess(PriceBreakdown result) {
                serverQuote = result;
                update();
            }

            @Override
            public void onError(ApiException error) {
                showNotice(error.getMessage());
            }
        });
    }

    // ---- shared -----------------------------------------------------------------

    /** Recomputes the totals, the summary and whether Continue is enabled. */
    private void update() {
        for (Runnable r : refreshers) {
            r.run();
        }
        PriceBreakdown p = currentBreakdown();
        long total = p.getTotal();
        bottomBar.setTotal(total);
        boolean valid = draft.isStepValid(step, catalog.getSettings());
        bottomBar.getButton().setDisabled(!valid);
        asideButton.setDisabled(!valid);
        fillSummaryLines(asideLines, p);
        asideLines.appendChild(Dom.el("div", "divider"));
        asideLines.appendChild(Dom.div("summary-total",
                Dom.text("span", "summary-total__label", step == OrderDraft.STEP_REVIEW ? "Total" : "Estimated total"),
                Dom.text("span", "summary-total__value", MoneyFormatter.format(total))));
    }

    private void next() {
        if (!draft.isStepValid(step, catalog.getSettings())) {
            return;
        }
        if (step < OrderDraft.STEP_REVIEW) {
            ctx.navigate("#/order/" + (step + 1));
        } else {
            placeOrder();
        }
    }

    private void back() {
        ctx.navigate(step == OrderDraft.STEP_SERVICE ? "#/home" : "#/order/" + (step - 1));
    }

    private void placeOrder() {
        OrderRequest request;
        try {
            request = draft.toRequest();
        } catch (ValidationException e) {
            showNotice(e.getMessage());
            return;
        }
        setBusy(true);
        notice.setHidden(true);
        Object proofFile = Browser.selectedFile(proofInput);
        ctx.getOrders().placeOrder(request, new Callback<Order>() {
            @Override
            public void onSuccess(Order order) {
                if (proofFile != null && request.getPayment() instanceof GCashPayment) {
                    uploadProof(order, proofFile);
                } else {
                    finish(order);
                }
            }

            @Override
            public void onError(ApiException error) {
                setBusy(false);
                showNotice(error.getMessage());
                Toast.error(error.getMessage());
            }
        });
    }

    private void uploadProof(Order order, Object file) {
        String userId = ctx.getCurrentUser().getId();
        ctx.getOrders().attachGcashProof(order, userId, file, Browser.fileType(file), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                finish(order);
            }

            @Override
            public void onError(ApiException error) {
                Toast.error("Your order was placed, but the screenshot did not upload: " + error.getMessage());
                finish(order);
            }
        });
    }

    private void finish(Order order) {
        setBusy(false);
        ctx.setLastPlacedOrder(order);
        draft.reset();
        ctx.navigate("#/order/placed");
    }

    private void setBusy(boolean busy) {
        bottomBar.getButton().setLoading(busy);
        asideButton.setLoading(busy);
    }

    private void showNotice(String message) {
        notice.setTextContent(message);
        notice.setHidden(false);
    }
}
