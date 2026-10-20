package com.laundryhub.ui;

import com.laundryhub.util.MoneyFormatter;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Sticky wizard footer: "Estimated total" + live amount on the left, primary button on the right.
 */
public class BottomBar extends Component {

    private final Button button;
    private long totalCentavos;
    private HTMLElement amount;

    /**
     * @param buttonLabel  "Continue" or "Place order"
     * @param totalCentavos starting total
     * @param onNext       button action
     */
    public BottomBar(String buttonLabel, long totalCentavos, Runnable onNext) {
        this.button = new Button(buttonLabel, Button.Variant.PRIMARY, onNext);
        this.totalCentavos = totalCentavos;
    }

    /** @param centavos new total */
    public void setTotal(long centavos) {
        totalCentavos = centavos;
        if (amount != null) {
            amount.setTextContent(MoneyFormatter.format(centavos));
        }
    }

    /** @return the button (to enable/disable or show loading) */
    public Button getButton() {
        return button;
    }

    @Override
    protected HTMLElement render() {
        amount = Dom.text("div", "bottom-bar__amount", MoneyFormatter.format(totalCentavos));
        amount.setAttribute("aria-live", "polite");
        HTMLElement left = Dom.div("bottom-bar__total", Dom.text("div", "bottom-bar__label", "Estimated total"), amount);
        HTMLElement footer = Dom.el("footer", "bottom-bar");
        return Dom.append(footer, left, button.getElement());
    }
}
