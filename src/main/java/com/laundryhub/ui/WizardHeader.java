package com.laundryhub.ui;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Order wizard header: back button, "Step N of 4", step title and 6 px progress bar.
 */
public class WizardHeader extends Component {

    private static final int PERCENT = 100;

    private final int step;
    private final int totalSteps;
    private final String title;
    private final Runnable onBack;

    /**
     * @param step       current step (1-based)
     * @param totalSteps number of steps
     * @param title      step title
     * @param onBack     back action
     */
    public WizardHeader(int step, int totalSteps, String title, Runnable onBack) {
        this.step = step;
        this.totalSteps = totalSteps;
        this.title = title;
        this.onBack = onBack;
    }

    @Override
    protected HTMLElement render() {
        HTMLElement row = Dom.div("wizard-header__row",
                new IconButton(Icons.BACK, "Back", false, onBack).getElement(),
                Dom.div("wizard-header__text",
                        Dom.text("div", "wizard-header__step", "Step " + step + " of " + totalSteps),
                        Dom.text("h1", "wizard-header__title", title)));
        HTMLElement fill = Dom.el("div", "progress__fill");
        fill.getStyle().setProperty("width", (step * PERCENT / totalSteps) + "%");
        HTMLElement bar = Dom.div("progress", fill);
        bar.setAttribute("role", "progressbar");
        bar.setAttribute("aria-valuemin", "0");
        bar.setAttribute("aria-valuemax", Integer.toString(totalSteps));
        bar.setAttribute("aria-valuenow", Integer.toString(step));
        HTMLElement header = Dom.el("header", "wizard-header");
        return Dom.append(header, row, bar);
    }
}
