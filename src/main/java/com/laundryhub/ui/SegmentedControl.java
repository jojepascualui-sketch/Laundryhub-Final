package com.laundryhub.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Tabs in a grey rounded track (Customer / Rider / Admin, Available / My jobs, Customers / Riders / Staff).
 */
public class SegmentedControl extends Component {

    private final List<String> labels;
    private final IntConsumer onChange;
    private final List<HTMLElement> buttons = new ArrayList<>();
    private int selected;
    private boolean compact;

    /**
     * @param labels   option labels
     * @param selected index selected at start
     * @param onChange called with the new index
     */
    public SegmentedControl(List<String> labels, int selected, IntConsumer onChange) {
        this.labels = new ArrayList<>(labels);
        this.selected = selected;
        this.onChange = onChange;
    }

    /**
     * @param value true for the inline admin style (auto width instead of equal columns)
     * @return this control
     */
    public SegmentedControl compact(boolean value) {
        this.compact = value;
        return this;
    }

    /** @return selected index */
    public int getSelected() {
        return selected;
    }

    /**
     * @param index option to select
     */
    public void select(int index) {
        selected = index;
        for (int i = 0; i < buttons.size(); i++) {
            Dom.toggleClass(buttons.get(i), "is-active", i == index);
            buttons.get(i).setAttribute("aria-selected", i == index ? "true" : "false");
        }
    }

    /**
     * @param index option index
     * @param label new label (e.g. "Available (4)")
     */
    public void setLabel(int index, String label) {
        labels.set(index, label);
        if (index < buttons.size()) {
            buttons.get(index).setTextContent(label);
        }
    }

    @Override
    protected HTMLElement render() {
        HTMLElement track = Dom.el("div", compact ? "segmented segmented--compact" : "segmented");
        track.setAttribute("role", "tablist");
        if (!compact) {
            track.getStyle().setProperty("grid-template-columns", "repeat(" + labels.size() + ", minmax(0, 1fr))");
        }
        buttons.clear();
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            HTMLElement b = Dom.text("button", "segmented__option", labels.get(i));
            b.setAttribute("type", "button");
            b.setAttribute("role", "tab");
            Dom.onClick(b, () -> {
                if (index != selected) {
                    select(index);
                    if (onChange != null) {
                        onChange.accept(index);
                    }
                }
            });
            buttons.add(b);
            track.appendChild(b);
        }
        select(selected);
        return track;
    }
}
