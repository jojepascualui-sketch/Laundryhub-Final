package com.laundryhub.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Confirmation dialog for actions that cannot be undone (cancel an order, give a job back).
 * A bottom sheet on phones and a centered dialog on desktop, with optional reason chips.
 * The confirm action receives the chosen reason (or null) and calls {@link #close()} or
 * {@link #fail(String)} when the server answers.
 */
public final class ConfirmSheet extends Component {

    // OOP: Encapsulation — the open/loading state and the chosen reason stay inside the sheet.
    private final String title;
    private final String message;
    private final List<String> reasons;
    private final String confirmLabel;
    private final String keepLabel;
    private final Consumer<String> onConfirm;
    private final List<Chip> chips = new ArrayList<>();
    private String reason;
    private Button confirm;
    private Button keep;
    private HTMLElement error;

    /**
     * @param title        heading, e.g. "Cancel this order?"
     * @param message      what will happen
     * @param reasons      reason chips, may be empty
     * @param confirmLabel e.g. "Yes, cancel order"
     * @param keepLabel    e.g. "Keep my order"
     * @param onConfirm    called with the chosen reason (may be null)
     */
    public ConfirmSheet(String title, String message, List<String> reasons, String confirmLabel,
                        String keepLabel, Consumer<String> onConfirm) {
        this.title = title;
        this.message = message;
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
        this.confirmLabel = confirmLabel;
        this.keepLabel = keepLabel;
        this.onConfirm = onConfirm;
    }

    /** Shows the sheet on top of the page. */
    public void open() {
        mount(HTMLDocument.current().getBody());
        Window.setTimeout(() -> getElement().getClassList().add("is-open"), 10);
        keep.getElement().focus();
    }

    /** Closes the sheet. */
    public void close() {
        unmount();
    }

    /** @param text error from the server; the sheet stays open so the user can read it */
    public void fail(String text) {
        confirm.setLoading(false);
        keep.setDisabled(false);
        error.setTextContent(text);
        error.getStyle().setProperty("display", "block");
    }

    @Override
    protected HTMLElement render() {
        HTMLElement panel = Dom.div("sheet__panel");
        panel.setAttribute("role", "alertdialog");
        panel.setAttribute("aria-modal", "true");
        panel.setAttribute("aria-labelledby", "sheet-title");
        HTMLElement heading = Dom.text("h2", "sheet__title", title);
        heading.setAttribute("id", "sheet-title");
        Dom.append(panel, Dom.div("sheet__handle"), Dom.div("sheet__icon", Dom.icon(Icons.ALERT, null)), heading,
                Dom.text("p", "sheet__message", message));

        if (!reasons.isEmpty()) {
            panel.appendChild(Dom.text("span", "sheet__label", "Reason (optional)"));
            HTMLElement grid = Dom.div("sheet__reasons");
            for (String r : reasons) {
                Chip[] holder = new Chip[1];
                holder[0] = new Chip(Chip.Style.FILTER, r, () -> choose(holder[0]));
                chips.add(holder[0]);
                grid.appendChild(holder[0].getElement());
            }
            panel.appendChild(grid);
        }

        error = Dom.text("div", "note note--warn sheet__error", "");
        error.getStyle().setProperty("display", "none");
        error.setAttribute("role", "alert");
        panel.appendChild(error);

        confirm = new Button(confirmLabel, Button.Variant.DANGER, null).block(true);
        confirm.setOnClick(() -> {
            confirm.setLoading(true);
            keep.setDisabled(true);
            error.getStyle().setProperty("display", "none");
            onConfirm.accept(reason);
        });
        keep = new Button(keepLabel, Button.Variant.OUTLINE, this::close).block(true);
        panel.appendChild(Dom.div("sheet__actions", keep.getElement(), confirm.getElement()));

        HTMLElement backdrop = Dom.div("sheet");
        backdrop.appendChild(panel);
        backdrop.addEventListener("click", e -> {
            if (isSelf(e, backdrop) && !keep.isDisabled()) {
                close();
            }
        });
        backdrop.addEventListener("keydown", e -> {
            if ("Escape".equals(keyOf(e)) && !keep.isDisabled()) {
                close();
            }
        });
        return backdrop;
    }

    private void choose(Chip chip) {
        boolean again = chip.getLabel().equals(reason);
        reason = again ? null : chip.getLabel();
        for (Chip c : chips) {
            c.setSelected(!again && c == chip);
        }
    }

    @JSBody(params = "e", script = "return e.key || '';")
    private static native String keyOf(Event e);

    @JSBody(params = {"e", "el"}, script = "return e.target === el;")
    private static native boolean isSelf(Event e, HTMLElement el);
}
