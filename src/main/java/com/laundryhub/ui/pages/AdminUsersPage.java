package com.laundryhub.ui.pages;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.Admin;
import com.laundryhub.model.Role;
import com.laundryhub.ui.AdminShell;
import com.laundryhub.ui.Badge;
import com.laundryhub.ui.Browser;
import com.laundryhub.ui.Button;
import com.laundryhub.ui.Checkbox;
import com.laundryhub.ui.Dom;
import com.laundryhub.ui.EmptyState;
import com.laundryhub.ui.Page;
import com.laundryhub.ui.SegmentedControl;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.TextInput;
import com.laundryhub.ui.Toast;
import com.laundryhub.util.Callback;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.TextUtils;
import com.laundryhub.util.Validators;
import com.laundryhub.web.AppContext;
import java.util.Arrays;
import java.util.Set;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

/**
 * Screen A4: Customers / Riders / Staff tables with Edit (name, mobile, role, job title, plate,
 * active). "+ Add rider/staff" finds an account that already signed up (by e-mail) and changes its
 * role — accounts can't be created from the browser without the secret key, which never goes in the site.
 */
public final class AdminUsersPage extends Page {

    private static final Role[] TABS = {Role.CUSTOMER, Role.RIDER, Role.STAFF};
    private static final String[] ADD_LABELS = {"+ Add customer", "+ Add rider", "+ Add staff"};

    private final AppContext ctx;
    private int tab;
    private boolean isAdmin;
    private HTMLElement tableBox;
    private HTMLElement panel;
    private Button addButton;

    /**
     * @param ctx app context
     */
    public AdminUsersPage(AppContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getTitle() {
        return "Customers & riders";
    }

    @Override
    public Set<Role> getAllowedRoles() {
        return roles(Role.ADMIN, Role.STAFF);
    }

    @Override
    protected HTMLElement render() {
        isAdmin = ctx.getCurrentUser() instanceof Admin && ((Admin) ctx.getCurrentUser()).canManageSettings();
        SegmentedControl tabs = new SegmentedControl(Arrays.asList("Customers", "Riders", "Staff"), tab, i -> {
            tab = i;
            addButton.setLabel(ADD_LABELS[i]);
            Dom.clear(panel);
            load();
        }).compact(true);
        addButton = new Button(ADD_LABELS[tab], Button.Variant.PRIMARY, this::showAdd);
        addButton.getElement().getClassList().add("btn--small");
        tableBox = Dom.div("table-scroll");
        panel = Dom.div("users-panel");
        HTMLElement card = Dom.div("card admin-card",
                Dom.div("card__head card__head--center", tabs.getElement(), addButton.getElement()), tableBox);
        load();
        return new AdminShell(ctx, "#/admin/users", "Accounts", "Customers & riders",
                Dom.div("admin-stack", card, panel)).getElement();
    }

    private void load() {
        Dom.clear(tableBox);
        tableBox.appendChild(new Spinner("Loading accounts…").getElement());
        ctx.getAdmin().loadUsers(TABS[tab], new Callback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray users) {
                drawTable(users);
            }

            @Override
            public void onError(ApiException error) {
                Dom.clear(tableBox);
                tableBox.appendChild(new EmptyState("Couldn't load accounts", error.getMessage(),
                        new Button("Try again", Button.Variant.PRIMARY, () -> load())).getElement());
            }
        });
    }

    private void drawTable(JsonArray users) {
        Dom.clear(tableBox);
        if (users.isEmpty()) {
            tableBox.appendChild(Dom.text("p", "muted users-empty", tab == 0 ? "No customers yet."
                    : tab == 1 ? "No riders yet. Use “+ Add rider”." : "No staff yet."));
            return;
        }
        String third = tab == 0 ? "Orders" : tab == 1 ? "Vehicle" : "Role";
        HTMLElement table = Dom.el("table", "admin-table");
        HTMLElement head = Dom.el("tr", null);
        for (String h : new String[] {"Name", "Contact", third, "Status", ""}) {
            head.appendChild(Dom.text("th", null, h));
        }
        HTMLElement thead = Dom.el("thead", null);
        thead.appendChild(head);
        HTMLElement tbody = Dom.el("tbody", null);
        for (JsonObject u : users.objects()) {
            HTMLElement tr = Dom.el("tr", null);
            Button edit = new Button("Edit", Button.Variant.OUTLINE, () -> showEdit(u));
            edit.getElement().getClassList().add("btn--small");
            Dom.append(tr,
                    Dom.text("td", "admin-table__strong", u.getString("full_name", "").isEmpty() ? "(no name)" : u.getString("full_name")),
                    Dom.text("td", null, contact(u)),
                    Dom.text("td", null, thirdColumn(u)),
                    cell(statusBadge(u).getElement()),
                    cell(edit.getElement()));
            tbody.appendChild(tr);
        }
        Dom.append(table, thead, tbody);
        tableBox.appendChild(table);
    }

    private String contact(JsonObject u) {
        String mobile = u.getString("mobile");
        if (tab == 2 || !Validators.hasText(mobile)) {
            return TextUtils.orEmpty(u.getString("email"));
        }
        return TextUtils.formatMobile(mobile);
    }

    private String thirdColumn(JsonObject u) {
        if (tab == 0) {
            long n = u.getLong("order_count", 0);
            return n + (n == 1 ? " order" : " orders");
        }
        if (tab == 1) {
            return u.isNull("plate_no") ? "Not set" : u.getString("plate_no");
        }
        String title = u.getString("job_title");
        return Validators.hasText(title) ? title : ("ADMIN".equals(u.getString("role")) ? "Admin" : "Staff");
    }

    private Badge statusBadge(JsonObject u) {
        if (!u.getBoolean("active", true)) {
            return new Badge("Inactive", Badge.Tone.CANCELLED, false);
        }
        if (tab == 1) {
            return u.getBoolean("on_duty", false) ? new Badge("On duty", Badge.Tone.DONE, false)
                    : new Badge("Off duty", Badge.Tone.PENDING, false);
        }
        return new Badge("Active", Badge.Tone.DONE, false);
    }

    private static HTMLElement cell(HTMLElement child) {
        HTMLElement td = Dom.el("td", null);
        td.appendChild(child);
        return td;
    }

    // ---- edit -----------------------------------------------------------------------

    private void showEdit(JsonObject u) {
        Dom.clear(panel);
        String id = u.getString("id");
        boolean self = ctx.getCurrentUser() != null && ctx.getCurrentUser().getId().equals(id);
        TextInput name = new TextInput("Full name", "", "text", null);
        name.setValue(u.getString("full_name", ""));
        TextInput mobile = new TextInput("Mobile number", "09XX XXX XXXX", "tel", "numeric");
        mobile.setValue(TextUtils.formatMobile(u.getString("mobile")));

        HTMLSelectElement role = (HTMLSelectElement) Dom.el("select", "select");
        for (Role r : Role.values()) {
            HTMLElement opt = Dom.text("option", null, r.name().charAt(0) + r.name().substring(1).toLowerCase());
            opt.setAttribute("value", r.name());
            role.appendChild(opt);
        }
        role.setValue(u.getString("role", "CUSTOMER"));
        role.setDisabled(!isAdmin || self);
        HTMLElement roleField = Dom.div("field", Dom.text("label", "field__label", "Role"), role);

        TextInput jobTitle = new TextInput("Job title", "e.g. Owner, Counter staff", "text", null);
        jobTitle.setValue(u.getString("job_title", ""));
        TextInput plate = new TextInput("Plate number", "e.g. NAB 1234", "text", null);
        plate.setValue(u.getString("plate_no", ""));
        Checkbox active = new Checkbox("Account is active (can sign in)", u.getBoolean("active", true), false);

        Button save = new Button("Save", Button.Variant.PRIMARY, null);
        Button cancel = new Button("Close", Button.Variant.OUTLINE, () -> Dom.clear(panel));
        save.setOnClick(() -> {
            if (Validators.hasText(mobile.getValue()) && !Validators.isMobile(mobile.getValue())) {
                mobile.setError("Enter the mobile number as 09XXXXXXXXX.");
                return;
            }
            JsonObject changes = new JsonObject()
                    .put("full_name", name.getValue().trim())
                    .put("mobile", Validators.hasText(mobile.getValue()) ? Validators.compact(mobile.getValue()) : null);
            if (isAdmin) {
                changes.put("role", role.getValue()).put("active", active.isChecked())
                        .put("job_title", jobTitle.getValue().trim().isEmpty() ? null : jobTitle.getValue().trim());
            }
            save.setLoading(true);
            ctx.getAdmin().updateUser(id, changes, new Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if ("RIDER".equals(role.getValue()) && tab == 1) {
                        ctx.getAdmin().setRiderPlate(id, plate.getValue().trim(), done(save));
                    } else {
                        done(save).onSuccess(null);
                    }
                }

                @Override
                public void onError(ApiException error) {
                    save.setLoading(false);
                    Toast.error(error.getMessage());
                }
            });
        });

        HTMLElement form = Dom.div("card admin-card users-edit",
                Dom.text("h2", "card__title", "Edit " + (u.getString("full_name", "").isEmpty() ? "account" : u.getString("full_name"))),
                Dom.text("p", "muted", TextUtils.orEmpty(u.getString("email"))),
                Dom.div("grid-2", name.getElement(), mobile.getElement()),
                Dom.div("grid-2", roleField, tab == 1 ? plate.getElement() : jobTitle.getElement()),
                active.getElement());
        if (!isAdmin) {
            form.appendChild(Dom.text("div", "notice notice--warn", "Only the owner (ADMIN) can change roles and status."));
            active.getElement().querySelector("input").setAttribute("disabled", "");
        } else if (self) {
            form.appendChild(Dom.text("p", "muted", "You can't change your own role."));
        }
        form.appendChild(Dom.div("row", save.getElement(), cancel.getElement()));
        panel.appendChild(form);
        form.scrollIntoView();
    }

    private Callback<Void> done(Button save) {
        return new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                save.setLoading(false);
                Toast.success("Account saved.");
                Dom.clear(panel);
                load();
            }

            @Override
            public void onError(ApiException error) {
                save.setLoading(false);
                Toast.error(error.getMessage());
            }
        };
    }

    // ---- add ------------------------------------------------------------------------

    private void showAdd() {
        Dom.clear(panel);
        if (tab == 0) {
            panel.appendChild(Dom.div("card admin-card",
                    Dom.text("h2", "card__title", "Add a customer"),
                    Dom.text("p", null, "Customers create their own account. Share this sign-up link:"),
                    Dom.text("p", "admin-link", Browser.appUrl() + "#/signup")));
            return;
        }
        Role target = tab == 1 ? Role.RIDER : Role.STAFF;
        String who = tab == 1 ? "rider" : "staff member";
        TextInput email = new TextInput("Their e-mail", "name@example.com", "email", null);
        Button find = new Button("Find account", Button.Variant.PRIMARY, null);
        HTMLElement result = Dom.div("stack stack--tight");
        find.setOnClick(() -> {
            if (!Validators.isEmail(email.getValue())) {
                email.setError("Enter a valid e-mail.");
                return;
            }
            find.setLoading(true);
            ctx.getAdmin().findProfileByEmail(email.getValue(), new Callback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject profile) {
                    find.setLoading(false);
                    Dom.clear(result);
                    if (profile == null) {
                        result.appendChild(Dom.text("div", "notice notice--warn",
                                "No account uses that e-mail. Ask them to sign up first at " + Browser.appUrl() + "#/signup"));
                        return;
                    }
                    String name = profile.getString("full_name", "").isEmpty() ? profile.getString("email") : profile.getString("full_name");
                    Button make = new Button("Make " + name + " a " + who, Button.Variant.PRIMARY, null);
                    make.setDisabled(!isAdmin);
                    make.setOnClick(() -> {
                        make.setLoading(true);
                        JsonObject changes = new JsonObject().put("role", target.name()).put("active", true);
                        ctx.getAdmin().updateUser(profile.getString("id"), changes, done(make));
                    });
                    result.appendChild(Dom.text("p", null, name + " · currently " + profile.getString("role", "CUSTOMER").toLowerCase()));
                    result.appendChild(make.getElement());
                }

                @Override
                public void onError(ApiException error) {
                    find.setLoading(false);
                    Toast.error(error.getMessage());
                }
            });
        });
        HTMLElement card = Dom.div("card admin-card users-edit",
                Dom.text("h2", "card__title", tab == 1 ? "Add a rider" : "Add staff"),
                Dom.text("p", "muted", "Ask the person to sign up on the website first, then find their account here."),
                email.getElement(), find.getElement(), result);
        if (!isAdmin) {
            card.appendChild(Dom.text("div", "notice notice--warn", "Only the owner (ADMIN) can add riders and staff."));
        }
        panel.appendChild(card);
        email.focus();
    }
}
