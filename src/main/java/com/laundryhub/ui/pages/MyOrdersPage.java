package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Order;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.Role;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.Spinner;
import com.laundryhub.util.Callback;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TimeUtils;
import com.laundryhub.web.AppContext;
import java.util.List;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Orders tab: the customer's orders, newest first, with status badge, total and payment status.
 * Tapping an order opens Track order.
 */
public final class MyOrdersPage extends Page {

    private final AppContext ctx;
    private HTMLElement list;

    /**
     * @param ctx app context
     */
    public MyOrdersPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "My orders";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected HTMLElement render() {
        HTMLElement page = Dom.div("page page--column");
        page.appendChild(Dom.text("div", "page__kicker", "Your laundry"));
        page.appendChild(Dom.text("h1", "page__title", "My orders"));
        list = Dom.div("stack");
        list.appendChild(new Spinner("Loading orders…").getElement());
        page.appendChild(list);
        load();
        return new CustomerShell("#/orders", ctx.getCurrentUser() == null ? null : ctx.getCurrentUser().getDisplayName(),
                ctx::signOut, page, true).getElement();
    }

    private void load() {
        ctx.getOrders().findMine(new Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                Dom.clear(list);
                if (orders.isEmpty()) {
                    list.appendChild(new EmptyState("No orders yet", "Your laundry orders will show up here.",
                            new Button("Start laundry order", Button.Variant.PRIMARY, () -> {
                                ctx.getOrderDraft().reset();
                                ctx.navigate("#/order/1");
                            })).getElement());
                    return;
                }
                for (Order order : orders) {
                    list.appendChild(card(order));
                }
            }

            @Override
            public void onError(ApiException error) {
                Dom.clear(list);
                list.appendChild(new EmptyState("Couldn't load your orders", error.getMessage(),
                        new Button("Try again", Button.Variant.PRIMARY, () -> {
                            Dom.clear(list);
                            list.appendChild(new Spinner("Loading orders…").getElement());
                            load();
                        })).getElement());
            }
        });
    }

    private HTMLElement card(Order order) {
        HTMLElement card = Dom.el("a", "order-card");
        card.setAttribute("href", "#/orders/" + order.getId());
        HTMLElement payment = Dom.text("span", "order-card__pay", order.getCustomerPaymentLabel());
        if (order.getPaymentStatus() == PaymentStatus.REJECTED) {
            payment.getClassList().add("order-card__pay--alert");
        } else if (order.getPaymentStatus() == PaymentStatus.PAID) {
            payment.getClassList().add("order-card__pay--ok");
        }
        Dom.append(card,
                Dom.div("order-card__top", Dom.text("span", "order-card__no", order.getOrderNo()),
                        Badge.of(order.getStatus()).getElement()),
                Dom.text("div", "order-card__line", order.getServiceType().getDisplayName() + " · " + order.getKg()
                        + " kg · " + MoneyFormatter.format(order.getTotal())),
                Dom.text("div", "order-card__meta", TimeUtils.manilaShortLabel(order.getCreatedAt())
                        + (order.getTimeSlot().isEmpty() ? "" : " · " + order.getTimeSlot())),
                payment);
        return card;
    }
}
