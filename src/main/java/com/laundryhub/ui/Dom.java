package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Short helpers for building DOM elements from Java.
 */
public final class Dom {

    private Dom() {
    }

    /** @return the page document */
    public static HTMLDocument doc() {
        return HTMLDocument.current();
    }

    /**
     * @param tag       tag name
     * @param className CSS classes, may be null
     * @return a new element
     */
    public static HTMLElement el(String tag, String className) {
        HTMLElement e = doc().createElement(tag);
        if (className != null && !className.isEmpty()) {
            e.setClassName(className);
        }
        return e;
    }

    /**
     * @param className CSS classes
     * @param children  child elements (nulls are skipped)
     * @return a div with the children
     */
    public static HTMLElement div(String className, HTMLElement... children) {
        HTMLElement e = el("div", className);
        append(e, children);
        return e;
    }

    /**
     * @param tag       tag name
     * @param className CSS classes, may be null
     * @param text      text content
     * @return a new element with text
     */
    public static HTMLElement text(String tag, String className, String text) {
        HTMLElement e = el(tag, className);
        e.setTextContent(text == null ? "" : text);
        return e;
    }

    /**
     * @param parent   parent element
     * @param children children to append (nulls are skipped)
     * @return the parent
     */
    public static HTMLElement append(HTMLElement parent, HTMLElement... children) {
        for (HTMLElement c : children) {
            if (c != null) {
                parent.appendChild(c);
            }
        }
        return parent;
    }

    /**
     * Inline SVG icon from {@link Icons}.
     *
     * @param svg       SVG markup (trusted constants only)
     * @param className extra CSS classes, may be null
     * @return a span containing the icon
     */
    public static HTMLElement icon(String svg, String className) {
        HTMLElement span = el("span", className == null ? "icon" : "icon " + className);
        span.setAttribute("aria-hidden", "true");
        span.setInnerHTML(svg);
        return span;
    }

    /**
     * @param e      element
     * @param action called on click
     */
    public static void onClick(HTMLElement e, Runnable action) {
        e.addEventListener("click", evt -> action.run());
    }

    /**
     * @param e element whose children are removed
     */
    public static void clear(HTMLElement e) {
        while (e.getFirstChild() != null) {
            e.removeChild(e.getFirstChild());
        }
    }

    /**
     * @param e element to detach from its parent (no-op when detached)
     */
    public static void remove(HTMLElement e) {
        if (e != null && e.getParentNode() != null) {
            e.getParentNode().removeChild(e);
        }
    }

    /**
     * @param e       element
     * @param cls     CSS class
     * @param enabled add when true, remove when false
     */
    public static void toggleClass(HTMLElement e, String cls, boolean enabled) {
        if (enabled) {
            e.getClassList().add(cls);
        } else {
            e.getClassList().remove(cls);
        }
    }

    /**
     * @param type        input type (text, email, password, tel, checkbox, file)
     * @param className   CSS classes
     * @return a new input element
     */
    public static HTMLInputElement input(String type, String className) {
        HTMLInputElement in = (HTMLInputElement) el("input", className);
        in.setType(type);
        return in;
    }
}
