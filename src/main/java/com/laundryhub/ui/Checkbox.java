package com.laundryhub.ui;

import java.util.function.Consumer;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Checkbox with its label (Remember me, Terms, Available, payment methods).
 */
public class Checkbox extends Component {

    private final String label;
    private final boolean small;
    private boolean initial;
    private Consumer<Boolean> onChange;
    private HTMLInputElement box;

    /**
     * @param label   text next to the box
     * @param checked starting state
     * @param small   true for the 18 px "Remember me" size, false for 20 px
     */
    public Checkbox(String label, boolean checked, boolean small) {
        this.label = label;
        this.initial = checked;
        this.small = small;
    }

    /**
     * @param listener called with the new state
     * @return this checkbox
     */
    public Checkbox onChange(Consumer<Boolean> listener) {
        this.onChange = listener;
        return this;
    }

    /** @return true when ticked */
    public boolean isChecked() {
        return box == null ? initial : box.isChecked();
    }

    /** @param value new state */
    public void setChecked(boolean value) {
        initial = value;
        if (box != null) {
            box.setChecked(value);
        }
    }

    /** @param message error text, or null to clear (shown by turning the label red) */
    public void setError(String message) {
        Dom.toggleClass(getElement(), "has-error", message != null);
    }

    @Override
    protected HTMLElement render() {
        HTMLElement wrap = Dom.el("label", small ? "checkbox checkbox--small" : "checkbox");
        box = Dom.input("checkbox", null);
        box.setChecked(initial);
        box.addEventListener("change", e -> {
            Dom.toggleClass(wrap, "has-error", false);
            if (onChange != null) {
                onChange.accept(box.isChecked());
            }
        });
        wrap.appendChild(box);
        wrap.appendChild(Dom.text("span", null, label));
        return wrap;
    }
}
