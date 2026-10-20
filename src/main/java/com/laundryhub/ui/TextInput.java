package com.laundryhub.ui;

import java.util.function.Consumer;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

/**
 * Labelled text field (min height 50, radius 14, border #C8D0CA) with an error line.
 */
public class TextInput extends Component {

    private static int nextId;

    private final String label;
    private final String placeholder;
    private final String type;
    private final String inputMode;
    private final String inputId;
    private String initialValue = "";
    private Consumer<String> onInput;
    private HTMLInputElement input;
    private HTMLElement errorLine;

    /**
     * @param label       label above the field
     * @param placeholder grey hint
     * @param type        text, email, password or tel
     * @param inputMode   mobile keyboard hint (e.g. "numeric"), may be null
     */
    public TextInput(String label, String placeholder, String type, String inputMode) {
        this.label = label;
        this.placeholder = placeholder;
        this.type = type;
        this.inputMode = inputMode;
        this.inputId = "f" + (++nextId);
    }

    /**
     * @param listener called on every keystroke with the current value
     * @return this input
     */
    public TextInput onInput(Consumer<String> listener) {
        this.onInput = listener;
        return this;
    }

    /** @return the current value, never null */
    public String getValue() {
        return input == null ? initialValue : input.getValue();
    }

    /** @param value new value */
    public void setValue(String value) {
        initialValue = value == null ? "" : value;
        if (input != null) {
            input.setValue(initialValue);
        }
    }

    /** @param message error to show under the field, or null to clear */
    public void setError(String message) {
        getElement();
        boolean show = message != null && !message.isEmpty();
        errorLine.setTextContent(show ? message : "");
        Dom.toggleClass(getElement(), "has-error", show);
        input.setAttribute("aria-invalid", show ? "true" : "false");
    }

    /** @param text new label text (e.g. "Staff email" on the Admin tab) */
    public void setLabel(String text) {
        getElement().querySelector(".field__label").setTextContent(text);
    }

    /** @param text new grey hint */
    public void setPlaceholder(String text) {
        getElement();
        input.setPlaceholder(text == null ? "" : text);
    }

    /**
     * @param value browser autofill hint, e.g. "current-password", "new-password", "name", "tel"
     * @return this input
     */
    public TextInput autocomplete(String value) {
        getElement();
        input.setAttribute("autocomplete", value);
        return this;
    }

    /** Puts the cursor in the field. */
    public void focus() {
        getElement();
        input.focus();
    }

    @Override
    protected HTMLElement render() {
        HTMLElement field = Dom.el("div", "field");
        HTMLElement lbl = Dom.text("label", "field__label", label);
        lbl.setAttribute("for", inputId);
        input = Dom.input(type, "field__input");
        input.setId(inputId);
        input.setPlaceholder(placeholder == null ? "" : placeholder);
        input.setValue(initialValue);
        if (inputMode != null) {
            input.setAttribute("inputmode", inputMode);
        }
        if ("email".equals(type)) {
            input.setAttribute("autocomplete", "email");
        }
        input.addEventListener("input", e -> {
            if (onInput != null) {
                onInput.accept(input.getValue());
            }
        });
        errorLine = Dom.el("div", "field__error");
        errorLine.setAttribute("role", "alert");
        return Dom.append(field, lbl, input, errorLine);
    }
}
