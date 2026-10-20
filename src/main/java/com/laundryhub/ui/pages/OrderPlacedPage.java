package com.laundryhub.ui.pages;

import com.laundryhub.model.Order;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.Role;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.Page;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.web.AppContext;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 06: "Order placed!" with the order number and a summary, then "Back to home".
 */
public final class OrderPlacedPage extends Page {

    private final AppContext ctx;

    /**
     * @param ctx app context (holds the order just placed)
     */
    public OrderPlacedPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Order placed";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected void onMount() {
        if (ctx.getLastPlacedOrder() == null) {
            ctx.navigate("#/home");
        }
    }

    @Override
    protected HTMLElement render() {
        Order order = ctx.getLastPlacedOrder();
        HTMLElement page = Dom.div("page page--narrow placed");
        if (order == null) {
            return page;
        }
        HTMLElement check = Dom.icon(Icons.CHECK.replace("width=\"18\" height=\"18\"", "width=\"44\" height=\"44\"")
                .replace("stroke-width=\"2\"", "stroke-width=\"3\""), "placed__check");
        HTMLElement card = Dom.div("card placed__card",
                row("Weight", order.getKg() + " kg"),
                row("Fabric soap", order.getSoapName()),
                row("Schedule", order.getTimeSlot()),
                row("Payment", order.getPaymentLabel()),
                Dom.el("div", "divider"),
                Dom.div("summary-total", Dom.text("span", "summary-total__label", "Total"),
                        Dom.text("span", "summary-total__value summary-total__value--sm", MoneyFormatter.format(order.getTotal()))));
        HTMLElement top = Dom.div("placed__top", check,
                Dom.text("h1", "placed__title", "Order placed!"),
                Dom.text("p", "placed__sub", "Order #" + order.getOrderNo() + " · " + order.getServiceType().getDisplayName()));
        Dom.append(page, top, card);
        if (order.getPaymentStatus() == PaymentStatus.PENDING_VERIFICATION) {
            page.appendChild(Dom.text("p", "placed__note",
                    "We'll check your GCash payment soon. You can follow it in Orders."));
        }
        Button home = new Button("Back to home", Button.Variant.DARK, () -> {
            ctx.setLastPlacedOrder(null);
            ctx.navigate("#/home");
        }).block(true);
        page.appendChild(Dom.div("placed__footer", home.getElement()));
        return new CustomerShell("#/home", ctx.getCurrentUser() == null ? null : ctx.getCurrentUser().getDisplayName(),
                ctx::signOut, page, false).getElement();
    }

    private static HTMLElement row(String label, String value) {
        return Dom.div("summary-row", Dom.text("span", "summary-row__label", label),
                Dom.text("span", "summary-row__value", value));
    }
}
