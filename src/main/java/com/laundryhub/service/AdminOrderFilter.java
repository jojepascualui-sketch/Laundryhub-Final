package com.laundryhub.service;

import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin Orders screen (A2): status filter chips with counts and the search box.
 * Pure Java, unit tested.
 */
public final class AdminOrderFilter {

    /** Chips in the order of the design (CANCELLED is shown only when there are some). */
    public static final OrderStatus[] CHIP_STATUSES = {
        OrderStatus.PENDING, OrderStatus.WASHING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY,
        OrderStatus.COMPLETED, OrderStatus.CANCELLED
    };

    private final List<Order> orders;

    /**
     * @param orders all loaded orders
     */
    public AdminOrderFilter(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
    }

    /** @return number of orders per status */
    public Map<OrderStatus, Integer> countByStatus() {
        Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s, 0);
        }
        for (Order o : orders) {
            counts.put(o.getStatus(), counts.get(o.getStatus()) + 1);
        }
        return counts;
    }

    /** @return all orders count ("All (6)") */
    public int total() {
        return orders.size();
    }

    /**
     * @param status only this status, or null for all
     * @param search text matched against order no., customer name, mobile and address (case-insensitive)
     * @return matching orders in the original order
     */
    public List<Order> filter(OrderStatus status, String search) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (status != null && o.getStatus() != status) {
                continue;
            }
            if (!q.isEmpty() && !matches(o, q)) {
                continue;
            }
            result.add(o);
        }
        return result;
    }

    private static boolean matches(Order o, String q) {
        return contains(o.getOrderNo(), q)
                || contains(o.getCustomerName(), q)
                || contains(o.getCustomerMobile(), q.replace(" ", ""))
                || (o.getLocation() != null && contains(o.getLocation().getAddress(), q))
                || contains(o.getAddressDetails(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }
}
