package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import com.laundryhub.ui.AuthLayout;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Checkbox;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.Icons;
import com.laundryhub.ui.InstallCard;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.SegmentedControl;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.Arrays;
import java.util.List;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Screens 00a / 00c: sign in with a Customer / Rider / Admin tab. The tab changes the heading,
 * the e-mail label and whether "Continue with Google" is shown (customers only).
 * After sign in each role goes to its own home.
 */
public final class SignInPage extends Page {

    private static final List<Role> TABS = Arrays.asList(Role.CUSTOMER, Role.RIDER, Role.ADMIN);
    private static final String ROLE_MISMATCH = "ROLE_MISMATCH:";

    private final AppContext ctx;
    private Role tab;
    private AuthLayout layout;
    private SegmentedControl tabs;
    private TextInput email;
    private TextInput password;
    private Checkbox remember;
    private Button signIn;
    private HTMLElement googleBlock;
    private HTMLElement notice;

    /**
     * @param ctx app context
     * @param tab tab selected at start (ADMIN for the admin sign-in link)
     */
    public SignInPage(AppContext ctx, Role tab) {
        this.ctx = ctx;
        this.tab = tab == null ? Role.CUSTOMER : tab;
    }

    @Override
    public String getTitle() {
        return tab == Role.ADMIN ? "Admin sign in" : "Sign in";
    }

    @Override
    protected void onMount() {
        User user = ctx.getCurrentUser();
        if (user != null) {
            ctx.navigate(user.getHomeRoute());
        }
    }

    @Override
    protected HTMLElement render() {
        tabs = new SegmentedControl(Arrays.asList("Customer", "Rider", "Admin"), TABS.indexOf(tab), i -> {
            tab = TABS.get(i);
            applyTab();
        });
        email = new TextInput("Email", "you@email.com", "email", null).autocomplete("username");
        password = new TextInput("Password", "At least 8 characters", "password", null).autocomplete("current-password");
        remember = new Checkbox("Remember me", false, true);
        HTMLElement forgot = Dom.text("button", "link-button", "Forgot password?");
        forgot.setAttribute("type", "button");
        Dom.onClick(forgot, this::forgotPassword);
        HTMLElement row = Dom.div("auth__row", remember.getElement(), forgot);

        signIn = new Button("Sign in", Button.Variant.PRIMARY, this::submit).block(true);
        signIn.getElement().setAttribute("type", "submit");
        signIn.getElement().getClassList().add("btn--tall");

        Button google = new Button("Continue with Google", Button.Variant.OUTLINE, Icons.GOOGLE, false, this::google)
                .block(true);
        googleBlock = Dom.div("stack", orDivider(), google.getElement());

        HTMLElement switchLine = Dom.div("auth__switch", Dom.text("span", null, "New to Laundry HUB?"),
                link("Sign up", "#/signup"));

        notice = Dom.el("div", "notice notice--error");
        notice.setAttribute("role", "alert");
        notice.setHidden(true);

        HTMLElement form = Dom.el("form", null);
        form.setAttribute("novalidate", "");
        form.addEventListener("submit", e -> {
            e.preventDefault();
            submit();
        });
        Dom.append(form, tabs.getElement(), notice, email.getElement(), password.getElement(), row,
                signIn.getElement(), googleBlock, switchLine, new InstallCard().getElement());

        if (!ctx.isConfigured()) {
            showNotice(ctx.getConfigError());
            signIn.setDisabled(true);
        }

        layout = new AuthLayout("", "", form);
        HTMLElement root = layout.getElement();
        applyTab();
        return root;
    }

    /** Updates the texts for the chosen tab (copied from the prototype). */
    private void applyTab() {
        switch (tab) {
            case RIDER:
                layout.setHeading("Rider sign in", "Accept bookings and deliver laundry.");
                email.setLabel("Rider email or mobile");
                email.setPlaceholder("you@email.com");
                break;
            case ADMIN:
                layout.setHeading("Admin sign in", "Manage orders, pricing and riders.");
                email.setLabel("Staff email");
                email.setPlaceholder("admin@laundryhub.ph");
                break;
            default:
                layout.setHeading("Welcome back", "Sign in to book your next laundry.");
                email.setLabel("Email");
                email.setPlaceholder("you@email.com");
        }
        googleBlock.setHidden(tab != Role.CUSTOMER);
        email.setError(null);
    }

    private void submit() {
        hideNotice();
        String login = email.getValue().trim();
        boolean riderMobile = tab == Role.RIDER && Validators.isMobile(login);
        boolean ok = true;
        if (!riderMobile && !Validators.isEmail(login)) {
            email.setError(tab == Role.RIDER ? "Enter your email or mobile number (09XXXXXXXXX)." : "Enter a valid email.");
            ok = false;
        } else {
            email.setError(null);
        }
        if (password.getValue().isEmpty()) {
            password.setError("Enter your password.");
            ok = false;
        } else {
            password.setError(null);
        }
        if (!ok || !ctx.isConfigured()) {
            return;
        }
        signIn.setLoading(true);
        ctx.getAuth().signIn(tab, login, password.getValue(), remember.isChecked(), new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                signIn.setLoading(false);
                ctx.setCurrentUser(user);
                Toast.success("Welcome, " + user.getFirstName() + "!");
                ctx.navigate(user.getHomeRoute());
            }

            @Override
            public void onError(ApiException error) {
                signIn.setLoading(false);
                if (error.getCode().startsWith(ROLE_MISMATCH)) {
                    Role correct = Role.fromCode(error.getCode().substring(ROLE_MISMATCH.length()));
                    tab = correct;
                    tabs.select(TABS.indexOf(correct));
                    applyTab();
                }
                showNotice(error.getMessage());
            }
        });
    }

    private void forgotPassword() {
        hideNotice();
        String login = email.getValue().trim();
        if (!Validators.isEmail(login)) {
            email.setError("Enter your email first, then tap Forgot password?");
            email.focus();
            return;
        }
        if (!ctx.isConfigured()) {
            return;
        }
        ctx.getAuth().sendPasswordReset(login, Browser.appUrl(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.success("We sent a password reset link to " + login + ".");
            }

            @Override
            public void onError(ApiException error) {
                showNotice(error.getMessage());
            }
        });
    }

    private void google() {
        if (ctx.isConfigured()) {
            Browser.go(ctx.getAuth().googleSignInUrl(Browser.appUrl()));
        }
    }

    private void showNotice(String message) {
        notice.setTextContent(message);
        notice.setHidden(false);
    }

    private void hideNotice() {
        notice.setHidden(true);
    }

    /** @return the "—— or ——" divider */
    static HTMLElement orDivider() {
        return Dom.div("auth__or", Dom.el("span", "auth__or-line"), Dom.text("span", null, "or"),
                Dom.el("span", "auth__or-line"));
    }

    /**
     * @param text  link text
     * @param route hash route
     * @return an accent text link
     */
    static HTMLElement link(String text, String route) {
        HTMLElement a = Dom.text("a", "link-button", text);
        a.setAttribute("href", route);
        return a;
    }
}
