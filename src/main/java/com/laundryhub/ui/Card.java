package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * White rounded container (radius 20, padding 18).
 */
public class Card extends Component {

    private final String extraClass;
    private final HTMLElement[] children;

    /**
     * @param extraClass extra CSS classes (e.g. "card--warn"), may be null
     * @param children   content
     */
    public Card(String extraClass, HTMLElement... children) {
        this.extraClass = extraClass;
        this.children = children;
    }

    /**
     * @param title section title shown at the top
     * @return a heading element for card content
     */
    public static HTMLElement title(String title) {
        return Dom.text("h3", "card__title", title);
    }

    @Override
    protected HTMLElement render() {
        return Dom.div(extraClass == null ? "card" : "card " + extraClass, children);
    }
}
