package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.ui.AuthLayout;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Opened from the "reset your password" e-mail (the link signs the user in for recovery).
 * Not in the mockups; uses the same frame as sign in.
 */
public final class ResetPasswordPage extends Page {

    private final AppContext ctx;
    private TextInput password;
    private TextInput confirm;
    private Button save;

    /**
     * @param ctx app context
     */
    public ResetPasswordPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Reset password";
    }

    @Override
    protected HTMLElement render() {
        HTMLElement form = Dom.el("form", null);
        form.setAttribute("novalidate", "");
        if (ctx.getCurrentUser() == null) {
            Dom.append(form,
                    Dom.text("div", "notice notice--warn",
                            "This reset link has expired or was already used. Request a new one from the sign-in page."),
                    new Button("Back to sign in", Button.Variant.DARK, () -> ctx.navigate("#/signin")).block(true).getElement());
            return new AuthLayout("Set a new password", "Choose a password you have not used before.", form).getElement();
        }
        password = new TextInput("New password", "At least 8 characters", "password", null).autocomplete("new-password");
        confirm = new TextInput("Confirm password", "Re-enter password", "password", null).autocomplete("new-password");
        save = new Button("Save password", Button.Variant.PRIMARY, this::submit).block(true);
        save.getElement().setAttribute("type", "submit");
        save.getElement().getClassList().add("btn--tall");
        form.addEventListener("submit", e -> {
            e.preventDefault();
            submit();
        });
        Dom.append(form, password.getElement(), confirm.getElement(), save.getElement());
        return new AuthLayout("Set a new password", "Choose a password you have not used before.", form).getElement();
    }

    private void submit() {
        boolean ok = Validators.isPassword(password.getValue());
        password.setError(ok ? null : "Use at least 8 characters.");
        boolean same = confirm.getValue().equals(password.getValue());
        confirm.setError(same ? null : "The passwords do not match.");
        if (!ok || !same) {
            return;
        }
        save.setLoading(true);
        ctx.getAuth().changePassword(password.getValue(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                save.setLoading(false);
                Toast.success("Your password was changed.");
                ctx.navigate(ctx.getCurrentUser().getHomeRoute());
            }

            @Override
            public void onError(ApiException error) {
                save.setLoading(false);
                password.setError(error.getMessage());
            }
        });
    }
}
