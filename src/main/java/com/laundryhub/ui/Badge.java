package com.laundryhub.ui;

import com.laundryhub.model.OrderStatus;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Small colored label. Badges always show text, never color alone.
 */
public class Badge extends Component {

    /** Badge colors (DESIGN_SPEC.md §1). */
    public enum Tone {
        /** Grey (Pending). */
        PENDING("pending"),
        /** Light blue (Washing, Pickup). */
        WASHING("washing"),
        /** Teal (Ready). */
        READY("ready"),
        /** Orange (Out for delivery, Return delivery, Collect cash). */
        OUT("out"),
        /** Green (Completed, Paid, Active, On duty). */
        DONE("done"),
        /** Red-ish grey (Cancelled, Rejected). */
        CANCELLED("cancelled");

        private final String key;

        Tone(String key) {
            this.key = key;
        }

        String getKey() {
            return key;
        }
    }

    private final String text;
    private final Tone tone;
    private final boolean large;

    /**
     * @param text  label
     * @param tone  color
     * @param large true for the bigger badge in detail headers
     */
    public Badge(String text, Tone tone, boolean large) {
        this.text = text;
        this.tone = tone;
        this.large = large;
    }

    /**
     * OOP: Polymorphism (overloading) — a status badge straight from an OrderStatus.
     *
     * @param status order status
     * @return a badge with the status text and colors
     */
    public static Badge of(OrderStatus status) {
        return new Badge(status.getLabel(), toneFor(status), false);
    }

    /**
     * @param status order status
     * @return the badge tone for it
     */
    public static Tone toneFor(OrderStatus status) {
        switch (status) {
            case WASHING: return Tone.WASHING;
            case READY: return Tone.READY;
            case OUT_FOR_DELIVERY: return Tone.OUT;
            case COMPLETED: return Tone.DONE;
            case CANCELLED: return Tone.CANCELLED;
            default: return Tone.PENDING;
        }
    }

    @Override
    protected HTMLElement render() {
        return Dom.text("span", "badge badge--" + tone.getKey() + (large ? " badge--large" : ""), text);
    }
}
