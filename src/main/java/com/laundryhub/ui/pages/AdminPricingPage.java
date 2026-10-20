package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.Admin;
import com.laundryhub.model.Location;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.ui.AdminShell;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Checkbox;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.GeoLocator;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
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
 * Screen A3: rates, fabric soaps, payment methods (and the shop's address/pin, which the
 * delivery radius needs). "Save changes" writes shop_settings and every edited soap.
 * Only ADMIN can save; STAFF sees the values read-only (RLS enforces it too).
 */
public final class AdminPricingPage extends Page {

    /** Largest QR picture accepted (the storage bucket allows 5 MB). */
    private static final double MAX_QR_BYTES = 5 * 1024 * 1024;

    private final AppContext ctx;
    private ShopSettings settings;
    private List<Soap> soaps = new ArrayList<>();
    private final List<SoapRow> soapRows = new ArrayList<>();
    private boolean canEdit;
    private HTMLElement body;

    private TextInput rate;
    private TextInput minKg;
    private TextInput fee;
    private TextInput radius;
    private Checkbox cash;
    private Checkbox gcash;
    private TextInput gcashNumber;
    private TextInput gcashName;
    private String qrPath;
    private HTMLElement qrBox;
    private TextInput address;
    private TextInput hours;
    private TextInput lat;
    private TextInput lng;
    private Button save;

    /** One editable soap row. */
    private static final class SoapRow {
        private final Soap soap;
        private final HTMLInputElement addOn;
        private final HTMLInputElement available;
        private final HTMLInputElement name;
        private final HTMLInputElement description;

        SoapRow(Soap soap, HTMLInputElement name, HTMLInputElement description, HTMLInputElement addOn,
                HTMLInputElement available) {
            this.soap = soap;
            this.name = name;
            this.description = description;
            this.addOn = addOn;
            this.available = available;
        }
    }

    /**
     * @param ctx app context
     */
    public AdminPricingPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Pricing & soaps";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.ADMIN, Role.STAFF);
    }

    @Override
    protected HTMLElement render() {
        canEdit = ctx.getCurrentUser() instanceof Admin && ((Admin) ctx.getCurrentUser()).canManageSettings();
        body = Dom.div("admin-stack");
        body.appendChild(new Spinner("Loading prices…").getElement());
        load();
        return new AdminShell(ctx, "#/admin/pricing", "Settings", "Pricing & soaps", body).getElement();
    }

    private void load() {
        ctx.getCatalog().loadSettings(new Callback<ShopSettings>() {
            @Override
            public void onSuccess(ShopSettings result) {
                settings = result;
                ctx.getCatalog().findAllSoaps(new Callback<List<Soap>>() {
                    @Override
                    public void onSuccess(List<Soap> list) {
                        soaps = list;
                        draw();
                    }

                    @Override
                    public void onError(ApiException error) {
                        showError(error);
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                showError(error);
            }
        });
    }

    private void showError(ApiException error) {
        Dom.clear(body);
        body.appendChild(new EmptyState("Couldn't load prices", error.getMessage(),
                new Button("Try again", Button.Variant.PRIMARY, this::load)).getElement());
    }

    // ---- drawing --------------------------------------------------------------------

    private void draw() {
        Dom.clear(body);
        if (!canEdit) {
            body.appendChild(Dom.text("div", "notice notice--warn",
                    "Only the owner (ADMIN) can change prices, soaps and payment settings. You can view them here."));
        }
        rate = number("Price per kilo (₱)", pesos(settings.getRatePerKg()), null);
        minKg = number("Minimum load (kg)", Integer.toString(settings.getMinKg()), "Orders below this are charged as the minimum.");
        fee = number("Pickup & delivery fee (₱)", pesos(settings.getDeliveryFee()), null);
        radius = number("Delivery radius (km)", trim(settings.getDeliveryRadiusKm()),
                "Pins outside this distance from the shop are rejected.");
        HTMLElement rates = Dom.div("card admin-card", Dom.text("h2", "card__title", "Rates"),
                rate.getElement(), minKg.getElement(), fee.getElement(), radius.getElement());

        HTMLElement right = Dom.div("admin-stack", soapsCard(), paymentCard(), shopCard());
        body.appendChild(Dom.div("pricing-grid", rates, right));

        save = new Button("Save changes", Button.Variant.PRIMARY, this::saveAll);
        save.setDisabled(!canEdit);
        body.appendChild(Dom.div("admin-actions", save.getElement()));
        setEnabled(canEdit);
    }

    private HTMLElement soapsCard() {
        soapRows.clear();
        HTMLElement table = Dom.el("table", "admin-table admin-table--soaps");
        HTMLElement head = Dom.el("tr", null);
        Dom.append(head, Dom.text("th", null, "Name"), Dom.text("th", null, "Add-on per kg"), Dom.text("th", null, "Available"));
        HTMLElement thead = Dom.el("thead", null);
        thead.appendChild(head);
        HTMLElement tbody = Dom.el("tbody", null);
        for (Soap soap : soaps) {
            tbody.appendChild(soapRow(soap));
        }
        Dom.append(table, thead, tbody);

        Button add = new Button("+ Add soap", Button.Variant.OUTLINE, () -> {
            tbody.appendChild(soapRow(null));
            soapRows.get(soapRows.size() - 1).name.focus();
        });
        add.getElement().getClassList().add("btn--small");
        add.setDisabled(!canEdit);
        return Dom.div("card admin-card",
                Dom.div("card__head", Dom.text("h2", "card__title", "Fabric soaps"), add.getElement()),
                Dom.div("table-scroll", table));
    }

    private HTMLElement soapRow(Soap soap) {
        HTMLInputElement name = Dom.input("text", "cell-input cell-input--name");
        name.setValue(soap == null ? "" : soap.getName());
        name.setPlaceholder("Soap name");
        name.setAttribute("aria-label", "Soap name");
        HTMLInputElement description = Dom.input("text", "cell-input cell-input--desc");
        description.setValue(soap == null ? "" : soap.getDescription());
        description.setPlaceholder("Short description");
        description.setAttribute("aria-label", "Description");
        HTMLInputElement addOn = Dom.input("number", "cell-input cell-input--money");
        addOn.setValue(soap == null ? "0" : pesos(soap.getAddOnPerKg()));
        addOn.setAttribute("min", "0");
        addOn.setAttribute("step", "0.5");
        addOn.setAttribute("aria-label", "Add-on per kg in pesos");
        HTMLInputElement available = Dom.input("checkbox", "cell-check");
        available.setChecked(soap == null || soap.isAvailable());
        available.setAttribute("aria-label", "Available");
        SoapRow row = new SoapRow(soap, name, description, addOn, available);
        soapRows.add(row);

        HTMLElement tr = Dom.el("tr", null);
        HTMLElement nameCell = Dom.el("td", null);
        Dom.append(nameCell, name, description);
        HTMLElement moneyCell = Dom.el("td", null);
        Dom.append(moneyCell, Dom.text("span", "cell-prefix", "₱"), addOn);
        HTMLElement checkCell = Dom.el("td", null);
        checkCell.appendChild(available);
        Dom.append(tr, nameCell, moneyCell, checkCell);
        boolean enabled = canEdit;
        name.setDisabled(!enabled);
        description.setDisabled(!enabled);
        addOn.setDisabled(!enabled);
        available.setDisabled(!enabled);
        return tr;
    }

    private HTMLElement paymentCard() {
        cash = new Checkbox("Cash (pay at counter or to rider)", settings.isCashEnabled(), false);
        gcash = new Checkbox("GCash", settings.isGcashEnabled(), false);
        gcashNumber = new TextInput("Shop GCash number", "09XX XXX XXXX", "tel", "numeric");
        gcashNumber.setValue(TextUtils.formatMobile(settings.getGcashNumber()));
        gcashName = new TextInput("GCash account name", "Name shown in GCash", "text", null);
        gcashName.setValue(TextUtils.orEmpty(settings.getGcashAccountName()));
        qrPath = settings.getGcashQrPath();
        qrBox = Dom.div("qr-admin");
        drawQr();
        return Dom.div("card admin-card", Dom.text("h2", "card__title", "Payment methods"),
                cash.getElement(), gcash.getElement(), gcashNumber.getElement(), gcashName.getElement(), qrBox);
    }

    /** GCash QR Ph: preview, upload and remove (uploads right away, separate from "Save changes"). */
    private void drawQr() {
        Dom.clear(qrBox);
        qrBox.appendChild(Dom.text("span", "field__label", "GCash QR Ph code"));
        HTMLElement img = Dom.el("img", "qr-admin__img");
        img.setAttribute("src", qrPath != null ? ctx.getCatalog().gcashQrUrl(qrPath) : ShopSettings.BUILT_IN_GCASH_QR);
        img.setAttribute("alt", "Shop GCash QR Ph code");
        qrBox.appendChild(img);
        qrBox.appendChild(Dom.text("span", "muted", qrPath != null
                ? "Customers see this uploaded QR."
                : "Customers see the QR built into the website. Upload another one to replace it."));
        if (!canEdit) {
            return;
        }
        qrBox.appendChild(Dom.text("span", "muted",
                "In GCash tap Receive → QR → Save QR (or download it from GCash for Business), then upload the picture here."));
        HTMLInputElement file = Dom.input("file", "file-input");
        file.setAttribute("accept", "image/png,image/jpeg,image/webp");
        file.setAttribute("aria-label", "GCash QR image");
        Button upload = new Button("Upload new QR", Button.Variant.OUTLINE, null);
        upload.getElement().getClassList().add("btn--small");
        upload.setOnClick(() -> uploadQr(file, upload));
        HTMLElement actions = Dom.div("row row--wrap", file, upload.getElement());
        if (qrPath != null) {
            Button remove = new Button("Use built-in QR", Button.Variant.LINK, null);
            remove.getElement().getClassList().add("btn--danger-link");
            remove.setOnClick(() -> removeQr(remove));
            actions.appendChild(remove.getElement());
        }
        qrBox.appendChild(actions);
    }

    private void uploadQr(HTMLInputElement input, Button button) {
        Object file = Browser.selectedFile(input);
        if (file == null) {
            Toast.error("Choose the QR picture first.");
            return;
        }
        String type = Browser.fileType(file);
        if (!type.equals("image/png") && !type.equals("image/jpeg") && !type.equals("image/webp")) {
            Toast.error("The QR must be a PNG, JPG or WebP picture.");
            return;
        }
        if (Browser.fileSize(file) > MAX_QR_BYTES) {
            Toast.error("The QR picture must be smaller than 5 MB.");
            return;
        }
        button.setLoading(true);
        ctx.getCatalog().uploadGcashQr(file, type, new Callback<String>() {
            @Override
            public void onSuccess(String path) {
                qrPath = path;
                ctx.getCatalogCache().invalidate();
                Toast.success("GCash QR saved. Customers see it when they pay with GCash.");
                drawQr();
            }

            @Override
            public void onError(ApiException error) {
                button.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }

    private void removeQr(Button button) {
        button.setLoading(true);
        ctx.getCatalog().removeGcashQr(new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                qrPath = null;
                ctx.getCatalogCache().invalidate();
                Toast.success("Customers now see the built-in GCash QR.");
                drawQr();
            }

            @Override
            public void onError(ApiException error) {
                button.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }

    private HTMLElement shopCard() {
        Location shop = settings.getShopLocation();
        address = new TextInput("Shop address", "Street, barangay, city", "text", null);
        address.setValue(settings.getShopAddress());
        hours = new TextInput("Opening hours", "8:00 AM – 8:00 PM", "text", null);
        hours.setValue(settings.getShopHours());
        lat = number("Shop latitude", shop == null ? "" : Double.toString(shop.getLat()), null);
        lng = number("Shop longitude", shop == null ? "" : Double.toString(shop.getLng()),
                "Tip: right-click the shop in Google Maps to copy its coordinates.");
        Button here = new Button("Use this device's location", Button.Variant.OUTLINE, null);
        here.getElement().getClassList().add("btn--small");
        here.setOnClick(() -> {
            here.setLoading(true);
            GeoLocator.locate(new GeoLocator.Listener() {
                @Override
                public void onPosition(double la, double ln) {
                    here.setLoading(false);
                    lat.setValue(Double.toString(Math.round(la * 1e6) / 1e6));
                    lng.setValue(Double.toString(Math.round(ln * 1e6) / 1e6));
                }

                @Override
                public void onError(String message) {
                    here.setLoading(false);
                    Toast.error(message);
                }
            });
        });
        here.setDisabled(!canEdit);
        return Dom.div("card admin-card", Dom.text("h2", "card__title", "Shop location"),
                Dom.text("p", "muted", "Shown to walk-in customers and used to check the delivery radius."),
                address.getElement(), hours.getElement(), Dom.div("grid-2", lat.getElement(), lng.getElement()),
                here.getElement());
    }

    private TextInput number(String label, String value, String hint) {
        TextInput input = new TextInput(label, "", "number", "decimal");
        input.setValue(value);
        if (hint != null) {
            input.getElement().appendChild(Dom.text("span", "field__hint", hint));
        }
        return input;
    }

    private void setEnabled(boolean enabled) {
        for (HTMLElement el : listOf(body.querySelectorAll(".field__input, .checkbox input"))) {
            if (enabled) {
                el.removeAttribute("disabled");
            } else {
                el.setAttribute("disabled", "");
            }
        }
    }

    private static List<HTMLElement> listOf(org.teavm.jso.dom.xml.NodeList<? extends HTMLElement> nodes) {
        List<HTMLElement> list = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add(nodes.get(i));
        }
        return list;
    }

    // ---- saving ---------------------------------------------------------------------

    private void saveAll() {
        ShopSettings next;
        List<Soap> changed;
        try {
            next = readSettings();
            changed = readSoaps();
        } catch (ValidationException e) {
            Toast.error(e.getMessage());
            return;
        }
        save.setLoading(true);
        ctx.getCatalog().saveSettings(next, new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                saveSoaps(changed, 0);
            }

            @Override
            public void onError(ApiException error) {
                save.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }

    private void saveSoaps(List<Soap> changed, int index) {
        if (index >= changed.size()) {
            save.setLoading(false);
            if (ctx.getCatalogCache() != null) {
                ctx.getCatalogCache().invalidate();
            }
            Toast.success("Changes saved. New orders use the new prices.");
            load();
            return;
        }
        ctx.getCatalog().saveSoap(changed.get(index), new Callback<Soap>() {
            @Override
            public void onSuccess(Soap result) {
                saveSoaps(changed, index + 1);
            }

            @Override
            public void onError(ApiException error) {
                save.setLoading(false);
                Toast.error(changed.get(index).getName() + ": " + error.getMessage());
            }
        });
    }

    private ShopSettings readSettings() {
        long ratePerKg = centavos(rate.getValue(), "Price per kilo");
        int min = (int) Math.round(parse(minKg.getValue(), "Minimum load"));
        long deliveryFee = centavos(fee.getValue(), "Delivery fee");
        double radiusKm = parse(radius.getValue(), "Delivery radius");
        String number = gcashNumber.getValue();
        if (Validators.hasText(number) && !Validators.isMobile(number)) {
            throw new ValidationException("Enter the shop GCash number as 09XXXXXXXXX.");
        }
        if (gcash.isChecked() && !Validators.hasText(number)) {
            throw new ValidationException("Add the shop GCash number, or turn GCash off.");
        }
        if (!cash.isChecked() && !gcash.isChecked()) {
            throw new ValidationException("Keep at least one payment method on.");
        }
        Location shop = new Location(parse(lat.getValue(), "Shop latitude"), parse(lng.getValue(), "Shop longitude"),
                address.getValue());
        return new ShopSettings(ratePerKg, min, deliveryFee, radiusKm, shop, hours.getValue(),
                Validators.hasText(number) ? number : null, gcashName.getValue().trim(), cash.isChecked(), gcash.isChecked(),
                qrPath);
    }

    private List<Soap> readSoaps() {
        List<Soap> changed = new ArrayList<>();
        for (SoapRow row : soapRows) {
            String name = row.name.getValue().trim();
            if (row.soap == null && name.isEmpty()) {
                continue; // an empty "+ Add soap" row is ignored
            }
            Soap edited = new Soap(row.soap == null ? 0 : row.soap.getId(), name, row.description.getValue(),
                    centavos(row.addOn.getValue(), name + " add-on"), row.available.isChecked());
            if (row.soap == null || !same(row.soap, edited)) {
                changed.add(edited);
            }
        }
        return changed;
    }

    private static boolean same(Soap a, Soap b) {
        return a.getName().equals(b.getName()) && a.getDescription().equals(b.getDescription())
                && a.getAddOnPerKg() == b.getAddOnPerKg() && a.isAvailable() == b.isAvailable();
    }

    private static double parse(String value, String field) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field + ": enter a number.");
        }
    }

    private static long centavos(String value, String field) {
        return MoneyFormatter.toCentavos(parse(value, field));
    }

    private static String pesos(long centavos) {
        return trim(MoneyFormatter.toPesos(centavos));
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }
}
