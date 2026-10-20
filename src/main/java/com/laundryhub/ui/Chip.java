package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Selectable chip: time slots (SLOT, radius 14, 2-column grid) or admin status filters
 * (FILTER, fully rounded pill with a count).
 */
public class Chip extends Component {

    /** Chip style. */
    public enum Style { SLOT, FILTER }

    private final Style style;
    private final String label;
    private final Runnable onClick;
    private boolean selected;

    /**
     * @param style   SLOT or FILTER
     * @param label   text, e.g. "Today, 2–4 PM" or "Pending (2)"
     * @param onClick action
     */
    public Chip(Style style, String label, Runnable onClick) {
        this.style = style;
        this.label = label;
        this.onClick = onClick;
    }

    /** @param value true to show as chosen */
    public void setSelected(boolean value) {
        selected = value;
        Dom.toggleClass(getElement(), "is-selected", value);
        getElement().setAttribute("aria-pressed", value ? "true" : "false");
    }

    /** @return the label */
    public String getLabel() {
        return label;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement b = Dom.text("button", style == Style.SLOT ? "chip" : "filter-chip", label);
        b.setAttribute("type", "button");
        b.setAttribute("aria-pressed", "false");
        if (onClick != null) {
            Dom.onClick(b, onClick);
        }
        Dom.toggleClass(b, "is-selected", selected);
        return b;
    }
}
