package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Role;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.IconButton;
import com.laundryhub.ui.InstallCard;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Toast;
import com.laundryhub.ui.TopNav;
import com.laundryhub.util.Callback;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.web.AppContext;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 01: greeting, "New order" card, price and minimum tiles, "How it works".
 * Rates come from shop_settings (₱30 / 8 kg until loaded).
 */
public final class HomePage extends Page {

    /** Decorative rings on the blue card (from the prototype). */
    static final String RINGS = "<svg width=\"160\" height=\"160\" viewBox=\"0 0 160 160\">"
            + "<circle cx=\"80\" cy=\"80\" r=\"70\" fill=\"none\" stroke=\"#fff\" stroke-width=\"14\"/>"
            + "<circle cx=\"80\" cy=\"80\" r=\"34\" fill=\"none\" stroke=\"#fff\" stroke-width=\"8\"/></svg>";

    private final AppContext ctx;
    private HTMLElement rateValue;
    private HTMLElement minValue;

    /**
     * @param ctx app context
     */
    public HomePage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected HTMLElement render() {
        HTMLElement page = Dom.div("page home");

        HTMLElement top = Dom.div("home__top", TopNav.brand(),
                new IconButton(Icons.BELL, "Notifications", false, () -> Toast.info("No new notifications.")).getElement());

        HTMLElement greeting = Dom.div("home__greeting",
                Dom.text("div", "home__hello", "Magandang araw!"),
                heroTitle());

        HTMLElement card = Dom.div("hero-card",
                Dom.icon(RINGS, "hero-card__rings"),
                Dom.text("div", "hero-card__kicker", "New order"),
                Dom.text("div", "hero-card__title", "Walk-in or we pick it up for you"),
                new Button("Start laundry order", Button.Variant.LIGHT, Icons.ARROW_RIGHT, true, this::startOrder).getElement());

        rateValue = Dom.text("span", null, MoneyFormatter.formatShort(ShopSettings.DEFAULT_RATE_PER_KG));
        minValue = Dom.text("span", "tile__value", ShopSettings.DEFAULT_MIN_KG + " kg");
        HTMLElement tiles = Dom.div("grid-2 home__tiles",
                Dom.div("tile", Dom.text("span", "tile__label", "Wash · Dry · Fold"),
                        Dom.div("tile__value", rateValue, Dom.text("span", "tile__unit", " /kg"))),
                Dom.div("tile tile--warn", Dom.text("span", "tile__label", "Minimum load"), minValue));

        HTMLElement how = Dom.div("stack home__how", Dom.text("h2", "section-title section-title--lg", "How it works"),
                howRow(1, "Choose walk-in or pickup & delivery"),
                howRow(2, "Set the kilos and pick your fabric soap"),
                howRow(3, "Pin your location, pay with Cash or GCash"));

        HTMLElement left = Dom.div("home__main", greeting, card);
        HTMLElement right = Dom.div("home__side", tiles, how, new InstallCard().getElement());
        Dom.append(page, top, Dom.div("home__grid", left, right));

        loadRates();
        return new CustomerShell("#/home", ctx.getCurrentUser() == null ? null : ctx.getCurrentUser().getDisplayName(),
                ctx::signOut, page, true).getElement();
    }

    private static HTMLElement heroTitle() {
        HTMLElement h = Dom.el("h1", "home__hero");
        h.appendChild(Dom.text("span", null, "Fresh clothes,"));
        h.appendChild(Dom.el("br", null));
        h.appendChild(Dom.text("span", null, "zero hassle."));
        return h;
    }

    private static HTMLElement howRow(int number, String text) {
        return Dom.div("how-row", Dom.text("span", "how-row__num", Integer.toString(number)),
                Dom.text("span", "how-row__text", text));
    }

    private void loadRates() {
        if (ctx.getCatalogCache() == null) {
            return;
        }
        ctx.getCatalogCache().get(new Callback<CatalogCache.Catalog>() {
            @Override
            public void onSuccess(CatalogCache.Catalog catalog) {
                ShopSettings s = catalog.getSettings();
                rateValue.setTextContent(MoneyFormatter.formatShort(s.getRatePerKg()));
                minValue.setTextContent(s.getMinKg() + " kg");
            }

            @Override
            public void onError(ApiException error) {
                Toast.error(error.getMessage());
            }
        });
    }

    private void startOrder() {
        ctx.getOrderDraft().reset();
        ctx.navigate("#/order/1");
    }
}
