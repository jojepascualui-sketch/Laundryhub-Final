package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Header with a round back button, a small kicker line and a title (screen 07 "Order #… / Track your laundry").
 */
public class BackHeader extends Component {

    private final String kicker;
    private final String title;
    private final Runnable onBack;

    /**
     * @param kicker small grey line, e.g. "Order #LH-0001"
     * @param title  big title
     * @param onBack back action
     */
    public BackHeader(String kicker, String title, Runnable onBack) {
        this.kicker = kicker;
        this.title = title;
        this.onBack = onBack;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement header = Dom.el("header", "back-header");
        Dom.append(header, new IconButton(Icons.BACK, "Back", false, onBack).getElement(),
                Dom.div("back-header__text", Dom.text("div", "back-header__kicker", kicker),
                        Dom.text("h1", "back-header__title", title)));
        return header;
    }
}
