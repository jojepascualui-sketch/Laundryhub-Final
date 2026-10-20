package com.laundryhub.service;

import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.util.TimeUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the "Order status" timeline of the Track order screen (07) from the order and its
 * order_status_history rows. Pure Java, unit tested.
 *
 * <p>Delivery: Order placed → Picked up → Washing &amp; drying → Out for delivery → Delivered.
 * Walk-in: Order placed → Washing &amp; drying → Ready for pickup → Picked up.
 * Done steps are filled, the first open step is "current", the rest are upcoming.
 */
public final class OrderTimeline {

    /** How a timeline step is drawn. */
    public enum State { DONE, CURRENT, UPCOMING }

    /** One row of the timeline. */
    public static final class Step {
        private final String label;
        private final String note;
        private final State state;
        private final String time;

        Step(String label, String note, State state, String time) {
            this.label = label;
            this.note = note;
            this.state = state;
            this.time = time;
        }

        /** @return title, e.g. "Picked up" */
        public String getLabel() {
            return label;
        }

        /** @return grey line under the title */
        public String getNote() {
            return note;
        }

        /** @return done, current or upcoming */
        public State getState() {
            return state;
        }

        /** @return when it happened (Philippine time label), or "" */
        public String getTime() {
            return time;
        }
    }

    private OrderTimeline() {
    }

    /**
     * @param order  the order
     * @param events its history, any order
     * @return the steps to draw (empty for cancelled orders)
     */
    public static List<Step> build(Order order, List<StatusEvent> events) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return Collections.emptyList();
        }
        List<String[]> defs = new ArrayList<>();
        List<Boolean> reached = new ArrayList<>();
        List<String> eventCodes = new ArrayList<>();
        OrderStatus s = order.getStatus();
        boolean washingOrLater = s == OrderStatus.WASHING || s == OrderStatus.READY
                || s == OrderStatus.OUT_FOR_DELIVERY || s == OrderStatus.COMPLETED;
        boolean readyOrLater = s == OrderStatus.READY || s == OrderStatus.OUT_FOR_DELIVERY || s == OrderStatus.COMPLETED;

        add(defs, reached, eventCodes, "Order placed", "We received your order", true, "PLACED");
        if (order.isDelivery()) {
            add(defs, reached, eventCodes, "Picked up", "Rider collected your laundry",
                    washingOrLater || has(events, "PICKED_UP"), "PICKED_UP");
            add(defs, reached, eventCodes, "Washing & drying", "Your clothes are being cleaned", readyOrLater, "WASHING");
            add(defs, reached, eventCodes, "Out for delivery", "Rider is heading to your pin",
                    s == OrderStatus.OUT_FOR_DELIVERY || s == OrderStatus.COMPLETED, "OUT_FOR_DELIVERY");
            add(defs, reached, eventCodes, "Delivered", "Enjoy your fresh laundry", s == OrderStatus.COMPLETED, "COMPLETED");
        } else {
            add(defs, reached, eventCodes, "Washing & drying", "Your clothes are being cleaned", readyOrLater, "WASHING");
            add(defs, reached, eventCodes, "Ready for pickup", "Come get it at the shop", readyOrLater, "READY");
            add(defs, reached, eventCodes, "Picked up", "Enjoy your fresh laundry", s == OrderStatus.COMPLETED, "COMPLETED");
        }

        List<Step> steps = new ArrayList<>();
        boolean currentGiven = false;
        for (int i = 0; i < defs.size(); i++) {
            State state;
            if (reached.get(i)) {
                state = State.DONE;
            } else if (!currentGiven) {
                state = State.CURRENT;
                currentGiven = true;
            } else {
                state = State.UPCOMING;
            }
            String time = state == State.DONE ? timeOf(events, eventCodes.get(i)) : "";
            steps.add(new Step(defs.get(i)[0], defs.get(i)[1], state, time));
        }
        return steps;
    }

    private static void add(List<String[]> defs, List<Boolean> reached, List<String> codes,
                            String label, String note, boolean done, String eventCode) {
        defs.add(new String[] {label, note});
        reached.add(done);
        codes.add(eventCode);
    }

    private static boolean has(List<StatusEvent> events, String code) {
        for (StatusEvent e : events) {
            if (code.equals(e.getEvent())) {
                return true;
            }
        }
        return false;
    }

    private static String timeOf(List<StatusEvent> events, String code) {
        String latest = null;
        for (StatusEvent e : events) {
            if (code.equals(e.getEvent()) && e.getCreatedAt() != null) {
                latest = e.getCreatedAt();
            }
        }
        return latest == null ? "" : TimeUtils.manilaShortLabel(latest);
    }
}
