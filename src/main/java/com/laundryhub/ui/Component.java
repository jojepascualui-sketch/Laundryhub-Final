package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * A piece of UI that builds its own DOM element.
 *
 * <p>OOP: Abstraction — every component and page is a Component; the router and parents only
 * call {@link #mount}/{@link #unmount}. OOP: Polymorphism — each subclass implements
 * {@link #render()} its own way.
 */
public abstract class Component {

    // OOP: Encapsulation — the element is created lazily and only exposed read-only.
    private HTMLElement element;
    private boolean rendering;
    private boolean dirty;

    /**
     * Builds the DOM for this component. Called once, or again by {@link #rerender()}.
     *
     * @return the root element
     */
    protected abstract HTMLElement render();

    /** @return the root element (rendered on first use) */
    public final HTMLElement getElement() {
        if (element == null) {
            rendering = true;
            element = render();
            rendering = false;
            if (dirty) {
                // rerender() was requested while the first render was running (e.g. cached data
                // arrived synchronously): render again with the new state.
                dirty = false;
                element = render();
            }
        }
        return element;
    }

    /**
     * Adds this component to a parent element.
     *
     * @param parent where to attach
     */
    public void mount(HTMLElement parent) {
        parent.appendChild(getElement());
        onMount();
    }

    /** Removes this component from the page and stops its timers. */
    public void unmount() {
        onUnmount();
        Dom.remove(element);
    }

    /** Replaces the element with a freshly rendered one (keeps the same position). */
    protected void rerender() {
        if (rendering || element == null) {
            dirty = true;
            return;
        }
        HTMLElement old = element;
        element = render();
        if (old != null && old.getParentNode() != null) {
            old.getParentNode().replaceChild(element, old);
        }
    }

    /** Called after the element is attached; override to start timers or listeners. */
    protected void onMount() {
    }

    /** Called before the element is removed; override to stop timers or listeners. */
    protected void onUnmount() {
    }
}
