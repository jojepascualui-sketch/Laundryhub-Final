package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.service.AuthService;
import com.laundryhub.ui.AuthLayout;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Checkbox;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screen 00b: create a customer account. Full name and mobile go to profiles through the
 * database trigger. When Supabase requires e-mail confirmation, a "Check your email" card is shown.
 */
public final class SignUpPage extends Page {

    private final AppContext ctx;
    private TextInput fullName;
    private TextInput mobile;
    private TextInput email;
    private TextInput password;
    private TextInput confirm;
    private Checkbox terms;
    private Button create;
    private HTMLElement notice;
    private HTMLElement form;

    /**
     * @param ctx app context
     */
    public SignUpPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Create account";
    }

    @Override
    protected void onMount() {
        if (ctx.getCurrentUser() != null) {
            ctx.navigate(ctx.getCurrentUser().getHomeRoute());
        }
    }

    @Override
    protected HTMLElement render() {
        fullName = new TextInput("Full name", "Juan Dela Cruz", "text", null).autocomplete("name");
        mobile = new TextInput("Mobile number", "09XX XXX XXXX", "tel", "numeric").autocomplete("tel");
        email = new TextInput("Email", "you@email.com", "email", null);
        password = new TextInput("Password", "At least 8 characters", "password", null).autocomplete("new-password");
        confirm = new TextInput("Confirm password", "Re-enter password", "password", null).autocomplete("new-password");
        terms = new Checkbox("I agree to the Terms of Service and Privacy Policy, including use of my location "
                + "for pickup and delivery.", false, false);

        create = new Button("Create account", Button.Variant.PRIMARY, this::submit).block(true);
        create.getElement().setAttribute("type", "submit");
        create.getElement().getClassList().add("btn--tall");
        Button google = new Button("Continue with Google", Button.Variant.OUTLINE, Icons.GOOGLE, false, () -> {
            if (ctx.isConfigured()) {
                Browser.go(ctx.getAuth().googleSignInUrl(Browser.appUrl()));
            }
        }).block(true);

        notice = Dom.el("div", "notice notice--error");
        notice.setAttribute("role", "alert");
        notice.setHidden(true);

        form = Dom.el("form", null);
        form.setAttribute("novalidate", "");
        form.addEventListener("submit", e -> {
            e.preventDefault();
            submit();
        });
        Dom.append(form, notice, fullName.getElement(), mobile.getElement(), email.getElement(),
                password.getElement(), confirm.getElement(), terms.getElement(), create.getElement(),
                SignInPage.orDivider(), google.getElement(),
                Dom.div("auth__switch", Dom.text("span", null, "Already have an account?"),
                        SignInPage.link("Sign in", "#/signin")));
        if (!ctx.isConfigured()) {
            notice.setTextContent(ctx.getConfigError());
            notice.setHidden(false);
            create.setDisabled(true);
        }
        return new AuthLayout("Create your account", "Book laundry in a few taps.", form).getElement();
    }

    private boolean validate() {
        boolean ok = true;
        ok &= check(fullName, Validators.hasText(fullName.getValue()), "Enter your full name.");
        ok &= check(mobile, Validators.isMobile(mobile.getValue()), "Enter your mobile number as 09XXXXXXXXX.");
        ok &= check(email, Validators.isEmail(email.getValue()), "Enter a valid email.");
        ok &= check(password, Validators.isPassword(password.getValue()), "Use at least 8 characters.");
        ok &= check(confirm, confirm.getValue().equals(password.getValue()) && !confirm.getValue().isEmpty(),
                "The passwords do not match.");
        if (!terms.isChecked()) {
            terms.setError("required");
            showNotice("Please agree to the Terms of Service and Privacy Policy.");
            ok = false;
        }
        return ok;
    }

    private static boolean check(TextInput input, boolean valid, String message) {
        input.setError(valid ? null : message);
        return valid;
    }

    private void submit() {
        notice.setHidden(true);
        if (!validate() || !ctx.isConfigured()) {
            return;
        }
        create.setLoading(true);
        ctx.getAuth().signUp(fullName.getValue(), mobile.getValue(), email.getValue().trim(), password.getValue(),
                new Callback<AuthService.SignUpResult>() {
                    @Override
                    public void onSuccess(AuthService.SignUpResult result) {
                        create.setLoading(false);
                        if (result.needsEmailConfirmation()) {
                            showCheckEmail();
                        } else {
                            ctx.setCurrentUser(result.getUser());
                            Toast.success("Welcome to Laundry HUB, " + result.getUser().getFirstName() + "!");
                            ctx.navigate(result.getUser().getHomeRoute());
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        create.setLoading(false);
                        showNotice(error.getMessage());
                    }
                });
    }

    private void showCheckEmail() {
        Dom.clear(form);
        Button back = new Button("Back to sign in", Button.Variant.DARK, () -> ctx.navigate("#/signin")).block(true);
        form.appendChild(new EmptyState("Check your email",
                "We sent a confirmation link to " + email.getValue().trim()
                        + ". Open it to finish creating your account.", back).getElement());
    }

    private void showNotice(String message) {
        notice.setTextContent(message);
        notice.setHidden(false);
    }
}
