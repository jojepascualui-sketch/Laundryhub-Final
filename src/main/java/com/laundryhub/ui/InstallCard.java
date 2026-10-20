package com.laundryhub.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * "Install Laundry HUB" card. Visible only when the browser can install the site (Chrome/Edge/
 * Android) or on iPhone/iPad Safari (with "Share → Add to Home Screen" steps). The × hides it on
 * this device.
 */
public class InstallCard extends Component {

    private HTMLElement root;
    private Runnable unsubscribe;

    @Override
    protected HTMLElement render() {
        root = Dom.div("install-card");
        root.setAttribute("role", "region");
        root.setAttribute("aria-label", "Install the app");
        unsubscribe = PwaInstall.addListener(() -> {
            if (!isInDocument(root)) {
                unsubscribe.run(); // the page was left: stop listening
                return;
            }
            refresh();
        });
        refresh();
        return root;
    }

    @JSBody(params = "el", script = "return document.body.contains(el);")
    private static native boolean isInDocument(HTMLElement el);

    private void refresh() {
        Dom.clear(root);
        boolean prompt = PwaInstall.canPrompt();
        boolean ios = PwaInstall.needsIosInstructions();
        root.setHidden(PwaInstall.isDismissed() || (!prompt && !ios));
        if (root.isHidden()) {
            return;
        }
        HTMLElement close = Dom.text("button", "install-card__close", "×");
        close.setAttribute("type", "button");
        close.setAttribute("aria-label", "Hide");
        Dom.onClick(close, () -> {
            PwaInstall.dismiss();
            root.setHidden(true);
        });
        HTMLElement text = Dom.div("install-card__text",
                Dom.text("span", "install-card__title", "Install Laundry HUB"),
                Dom.text("span", "install-card__sub", prompt
                        ? "Add it to your home screen or desktop. It opens like an app."
                        : "On iPhone: tap Share ⬆︎, then “Add to Home Screen”."));
        Dom.append(root, Dom.icon(Icons.LOGO, "install-card__icon"), text);
        if (prompt) {
            Button install = new Button("Install app", Button.Variant.PRIMARY, Icons.DOWNLOAD, false,
                    () -> PwaInstall.prompt(() -> Toast.success("Installed! Open Laundry HUB from your home screen.")));
            install.getElement().getClassList().add("btn--small");
            root.appendChild(install.getElement());
        }
        root.appendChild(close);
    }
}
