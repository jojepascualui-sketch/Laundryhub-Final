package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Frame of the sign-in, sign-up and reset screens: blue header with the logo tile, heading and
 * subheading, then the form. Phone: full screen like the mockup. Tablet/desktop: a centered
 * card (max 440 px) on the light background.
 */
public class AuthLayout extends Component {

    private static final String RINGS = "<svg width=\"220\" height=\"220\" viewBox=\"0 0 160 160\">"
            + "<circle cx=\"80\" cy=\"80\" r=\"70\" fill=\"none\" stroke=\"#fff\" stroke-width=\"14\"/>"
            + "<circle cx=\"80\" cy=\"80\" r=\"34\" fill=\"none\" stroke=\"#fff\" stroke-width=\"8\"/></svg>";

    private final HTMLElement body;
    private String heading;
    private String subheading;
    private HTMLElement headingEl;
    private HTMLElement subheadingEl;

    /**
     * @param heading    big white title, e.g. "Welcome back"
     * @param subheading line under it
     * @param body       the form content
     */
    public AuthLayout(String heading, String subheading, HTMLElement body) {
        this.heading = heading;
        this.subheading = subheading;
        this.body = body;
    }

    /**
     * @param newHeading    new title
     * @param newSubheading new line under it
     */
    public void setHeading(String newHeading, String newSubheading) {
        heading = newHeading;
        subheading = newSubheading;
        if (headingEl != null) {
            headingEl.setTextContent(newHeading);
            subheadingEl.setTextContent(newSubheading);
        }
    }

    @Override
    protected HTMLElement render() {
        headingEl = Dom.text("h1", "auth__heading", heading);
        subheadingEl = Dom.text("p", "auth__sub", subheading);
        HTMLElement hero = Dom.el("header", "auth__hero");
        HTMLElement rings = Dom.icon(RINGS, "auth__rings");
        Dom.append(hero, rings, Dom.icon(Icons.LOGO.replace("width=\"22\" height=\"22\"", "width=\"28\" height=\"28\""),
                "auth__logo"), headingEl, subheadingEl);
        body.getClassList().add("auth__body");
        HTMLElement card = Dom.div("auth__card", hero, body);
        return Dom.div("auth", card);
    }
}
