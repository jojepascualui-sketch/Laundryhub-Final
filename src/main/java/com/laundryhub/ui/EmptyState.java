package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * White card shown when a list is empty or something failed (same style as the "You're off duty" card).
 */
public class EmptyState extends Component {

    private final String title;
    private final String message;
    private final Button action;

    /**
     * @param title   bold line, may be null
     * @param message explanation
     * @param action  optional button (e.g. "Try again"), may be null
     */
    public EmptyState(String title, String message, Button action) {
        this.title = title;
        this.message = message;
        this.action = action;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement box = Dom.div("empty-state");
        if (title != null) {
            box.appendChild(Dom.text("div", "empty-state__title", title));
        }
        box.appendChild(Dom.text("div", "empty-state__text", message));
        if (action != null) {
            box.appendChild(action.getElement());
        }
        return box;
    }
}
