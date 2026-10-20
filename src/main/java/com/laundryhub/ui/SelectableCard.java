package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * A card the user taps to choose it: service, payment method (ROW layout with a radio dot)
 * or fabric soap (TILE layout in a 2×2 grid). Selected = accent border + light blue background.
 */
public class SelectableCard extends Component {

    /** Card layout. */
    public enum Layout {
        /** Icon left, text, radio dot right (screens 02, 05). */
        ROW,
        /** Small icon on top, compact text (soap grid, screen 03). */
        TILE
    }

    private final Layout layout;
    private final String iconSvg;
    private final String badgeText;
    private final String tint;
    private final String iconColor;
    private final String title;
    private final String description;
    private final String meta;
    private final Runnable onSelect;
    private boolean selected;
    private boolean disabled;

    /**
     * @param layout      ROW or TILE
     * @param iconSvg     icon from {@link Icons}, or null to show {@code badgeText}
     * @param badgeText   text in the icon tile (e.g. "₱", "G"), used when iconSvg is null
     * @param tint        icon tile background color (CSS), may be null for the default tint
     * @param iconColor   icon color (CSS), may be null for the default
     * @param title       title
     * @param description description, may be null
     * @param meta        accent line (fee / price), may be null
     * @param onSelect    called when tapped
     */
    public SelectableCard(Layout layout, String iconSvg, String badgeText, String tint, String iconColor,
                          String title, String description, String meta, Runnable onSelect) {
        this.layout = layout;
        this.iconSvg = iconSvg;
        this.badgeText = badgeText;
        this.tint = tint;
        this.iconColor = iconColor;
        this.title = title;
        this.description = description;
        this.meta = meta;
        this.onSelect = onSelect;
    }

    /** @param value true to show as chosen */
    public void setSelected(boolean value) {
        selected = value;
        Dom.toggleClass(getElement(), "is-selected", value);
        getElement().setAttribute("aria-checked", value ? "true" : "false");
    }

    /** @return true when chosen */
    public boolean isSelected() {
        return selected;
    }

    /** @param value true to grey out (e.g. GCash turned off) */
    public void setDisabled(boolean value) {
        disabled = value;
        if (value) {
            getElement().setAttribute("disabled", "");
        } else {
            getElement().removeAttribute("disabled");
        }
    }

    @Override
    protected HTMLElement render() {
        HTMLElement card = Dom.el("button", "select-card select-card--" + layout.name().toLowerCase());
        card.setAttribute("type", "button");
        card.setAttribute("role", "radio");
        card.setAttribute("aria-checked", "false");

        HTMLElement tile = iconSvg != null ? Dom.icon(iconSvg, "select-card__icon")
                : Dom.text("span", "select-card__icon select-card__icon--text", badgeText);
        if (tint != null) {
            tile.getStyle().setProperty("background", tint);
        }
        if (iconColor != null) {
            tile.getStyle().setProperty("color", iconColor);
        }
        card.appendChild(tile);

        HTMLElement body = Dom.el("span", "select-card__body");
        body.appendChild(Dom.text("span", "select-card__title", title));
        if (description != null) {
            body.appendChild(Dom.text("span", "select-card__desc", description));
        }
        if (meta != null) {
            body.appendChild(Dom.text("span", "select-card__meta", meta));
        }
        card.appendChild(body);
        if (layout == Layout.ROW) {
            card.appendChild(Dom.el("span", "select-card__radio"));
        }
        card.addEventListener("click", e -> {
            if (!disabled && onSelect != null) {
                onSelect.run();
            }
        });
        Dom.toggleClass(card, "is-selected", selected);
        return card;
    }
}
