package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Loading indicator with an optional caption.
 */
public class Spinner extends Component {

    private final String caption;

    /**
     * @param caption text under the spinner, e.g. "Loading orders…", may be null
     */
    public Spinner(String caption) {
        this.caption = caption;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement box = Dom.div("loading", Dom.el("span", "spinner"));
        box.setAttribute("role", "status");
        box.appendChild(Dom.text("span", caption == null ? "visually-hidden" : "loading__text",
                caption == null ? "Loading" : caption));
        return box;
    }
}
