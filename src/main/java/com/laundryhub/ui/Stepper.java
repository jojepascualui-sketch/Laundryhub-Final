package com.laundryhub.ui;

import java.util.function.IntConsumer;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * The weight stepper: round − / + buttons around a big number. − is disabled at the minimum.
 */
public class Stepper extends Component {

    private final int min;
    private final int max;
    private final String unit;
    private final IntConsumer onChange;
    private int value;
    private HTMLElement minus;
    private HTMLElement plus;
    private HTMLElement number;

    /**
     * @param value    starting value
     * @param min      lowest value (− disabled there)
     * @param max      highest value
     * @param unit     text under the number, e.g. "kilograms"
     * @param onChange called with the new value
     */
    public Stepper(int value, int min, int max, String unit, IntConsumer onChange) {
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.onChange = onChange;
        this.value = Math.max(min, Math.min(max, value));
    }

    /** @return current value */
    public int getValue() {
        return value;
    }

    @Override
    protected HTMLElement render() {
        minus = Dom.text("button", "stepper__btn", "−");
        minus.setAttribute("type", "button");
        minus.setAttribute("aria-label", "Decrease " + unit);
        plus = Dom.text("button", "stepper__btn stepper__btn--plus", "+");
        plus.setAttribute("type", "button");
        plus.setAttribute("aria-label", "Increase " + unit);
        number = Dom.text("div", "stepper__value", Integer.toString(value));
        number.setAttribute("aria-live", "polite");
        Dom.onClick(minus, () -> change(-1));
        Dom.onClick(plus, () -> change(1));
        HTMLElement center = Dom.div("stepper__center", number, Dom.text("div", "stepper__unit", unit));
        HTMLElement root = Dom.div("stepper", minus, center, plus);
        updateButtons();
        return root;
    }

    private void change(int delta) {
        int next = value + delta;
        if (next < min || next > max) {
            return;
        }
        value = next;
        number.setTextContent(Integer.toString(value));
        updateButtons();
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    private void updateButtons() {
        setDisabled(minus, value <= min);
        setDisabled(plus, value >= max);
    }

    private static void setDisabled(HTMLElement b, boolean disabled) {
        if (disabled) {
            b.setAttribute("disabled", "");
        } else {
            b.removeAttribute("disabled");
        }
    }
}
