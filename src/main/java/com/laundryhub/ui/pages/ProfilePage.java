package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.CustomerShell;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.InstallCard;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.TextUtils;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Profile tab (not in the mockups; same style): edit name and mobile, sign out.
 * Google sign-ups have no mobile yet, so riders could not call them; this page lets them add it.
 */
public final class ProfilePage extends Page {

    private final AppContext ctx;
    private TextInput name;
    private TextInput mobile;
    private Button save;

    /**
     * @param ctx app context
     */
    public ProfilePage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Profile";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.CUSTOMER);
    }

    @Override
    protected HTMLElement render() {
        User user = ctx.getCurrentUser();
        HTMLElement page = Dom.div("page page--column");
        page.appendChild(Dom.text("h1", "page__title", "Profile"));
        if (user == null) {
            return page;
        }
        name = new TextInput("Full name", "Juan Dela Cruz", "text", null).autocomplete("name");
        name.setValue(user.getFullName());
        mobile = new TextInput("Mobile number", "09XX XXX XXXX", "tel", "numeric").autocomplete("tel");
        mobile.setValue(TextUtils.formatMobile(user.getMobile()));
        save = new Button("Save changes", Button.Variant.PRIMARY, this::save).block(true);

        HTMLElement card = Dom.div("card",
                Dom.div("profile__head", Dom.text("span", "profile__avatar", user.getInitials()),
                        Dom.div("profile__who", Dom.text("span", "profile__name", user.getDisplayName()),
                                Dom.text("span", "muted", TextUtils.orEmpty(user.getEmail())))),
                name.getElement(), mobile.getElement(), save.getElement());
        if (!Validators.hasText(user.getMobile())) {
            page.appendChild(Dom.text("div", "notice notice--warn",
                    "Add your mobile number so our rider can call you for pickup and delivery."));
        }
        page.appendChild(card);
        page.appendChild(new InstallCard().getElement());
        page.appendChild(new Button("Sign out", Button.Variant.OUTLINE, ctx::signOut).block(true).getElement());
        return new CustomerShell("#/profile", user.getDisplayName(), ctx::signOut, page, true).getElement();
    }

    private void save() {
        boolean nameOk = Validators.hasText(name.getValue());
        boolean mobileOk = !Validators.hasText(mobile.getValue()) || Validators.isMobile(mobile.getValue());
        name.setError(nameOk ? null : "Enter your full name.");
        mobile.setError(mobileOk ? null : "Enter your mobile number as 09XXXXXXXXX.");
        if (!nameOk || !mobileOk) {
            return;
        }
        save.setLoading(true);
        String id = ctx.getCurrentUser().getId();
        ctx.getProfiles().updateOwnProfile(id, name.getValue(), mobile.getValue(), new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                ctx.getProfiles().findUser(id, new Callback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        save.setLoading(false);
                        ctx.setCurrentUser(user);
                        Toast.success("Profile saved.");
                        rerender();
                    }

                    @Override
                    public void onError(ApiException error) {
                        save.setLoading(false);
                        Toast.success("Profile saved.");
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                save.setLoading(false);
                Toast.error(error.getMessage());
            }
        });
    }
}
