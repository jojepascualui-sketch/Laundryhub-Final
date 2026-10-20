package com.laundryhub.web;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.AuthSession;
import com.laundryhub.model.Role;
import com.laundryhub.model.User;
import com.laundryhub.repository.SupabaseClient;
import com.laundryhub.ui.PwaInstall;
import com.laundryhub.ui.Responsive;
import com.laundryhub.ui.Router;
import com.laundryhub.ui.Spinner;
import com.laundryhub.ui.Toast;
import com.laundryhub.ui.pages.AdminDashboardPage;
import com.laundryhub.ui.pages.AdminOrdersPage;
import com.laundryhub.ui.pages.AdminPricingPage;
import com.laundryhub.ui.pages.AdminReportsPage;
import com.laundryhub.ui.pages.AdminUsersPage;
import com.laundryhub.ui.pages.ComponentGalleryPage;
import com.laundryhub.ui.pages.HomePage;
import com.laundryhub.ui.pages.MyOrdersPage;
import com.laundryhub.ui.pages.TrackOrderPage;
import com.laundryhub.ui.pages.OrderPlacedPage;
import com.laundryhub.ui.pages.OrderWizardPage;
import com.laundryhub.ui.pages.ProfilePage;
import com.laundryhub.ui.pages.ResetPasswordPage;
import com.laundryhub.ui.pages.RiderHomePage;
import com.laundryhub.ui.pages.RiderJobPage;
import com.laundryhub.ui.pages.SignInPage;
import com.laundryhub.ui.pages.SignUpPage;
import com.laundryhub.util.Callback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Entry point compiled by TeaVM into {@code web-dist/js/app.js}; {@code index.html} calls main().
 * Wires the app together and starts the router.
 */
public final class App {


    /**
     * @param args unused
     */
    public static void main(String[] args) {
        Responsive.init();
        PwaInstall.init();
        HTMLElement outlet = HTMLDocument.current().getElementById("app");
        AppContext ctx = AppContext.create();
        Router router = new Router(outlet, ctx::getCurrentUser);
        ctx.setRouter(router);
        registerRoutes(router, ctx);

        if (!ctx.isConfigured()) {
            router.start();
            return;
        }

        SupabaseClient client = ctx.getClient();
        client.addSessionListener(session -> {
            if (session == null && ctx.getCurrentUser() != null) {
                ctx.setCurrentUser(null);
                router.navigate("#/signin");
            }
        });

        new Spinner("Loading Laundry HUB…").mount(outlet);
        String hash = Window.current().getLocation().getHash();
        String fragment = hash.startsWith("#") ? hash.substring(1) : hash;
        String redirectError = SupabaseClient.getRedirectError(fragment);
        if (redirectError != null) {
            Toast.error(redirectError);
            Window.current().getLocation().setHash("#/signin");
        }

        if (SupabaseClient.isAuthRedirect(fragment)) {
            boolean recovery = SupabaseClient.isRecoveryRedirect(fragment);
            client.completeRedirectSignIn(fragment, new Callback<AuthSession>() {
                @Override
                public void onSuccess(AuthSession session) {
                    loadUserThenStart(ctx, router, recovery ? "#/reset-password" : "#/");
                }

                @Override
                public void onError(ApiException error) {
                    Toast.error(error.getMessage());
                    Window.current().getLocation().setHash("#/signin");
                    router.start();
                }
            });
        } else if (client.isSignedIn()) {
            loadUserThenStart(ctx, router, null);
        } else {
            router.start();
        }
    }

    private static void loadUserThenStart(AppContext ctx, Router router, String goTo) {
        ctx.getAuth().loadCurrentUser(new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                ctx.setCurrentUser(user);
                if (goTo != null) {
                    Window.current().getLocation().setHash(goTo);
                }
                router.start();
            }

            @Override
            public void onError(ApiException error) {
                // AuthService already signed out when the profile is missing or turned off.
                if (!error.isUnauthorized()) {
                    Toast.error(error.getMessage());
                }
                if (goTo != null) {
                    Window.current().getLocation().setHash("#/signin");
                }
                router.start();
            }
        });
    }

    private static void registerRoutes(Router router, AppContext ctx) {
        router
            .add("/dev/components", p -> new ComponentGalleryPage())
            // Phase 3
            .add("/signin", p -> new SignInPage(ctx, Role.CUSTOMER))
            .add("/signin/:role", p -> new SignInPage(ctx, tabFromRoute(p.get("role"))))
            .add("/signup", p -> new SignUpPage(ctx))
            .add("/reset-password", p -> new ResetPasswordPage(ctx))
            // Phase 4–6: customer
            .add("/home", p -> new HomePage(ctx))
            .add("/order/placed", p -> new OrderPlacedPage(ctx))
            .add("/order/:step", p -> new OrderWizardPage(ctx, parseStep(p.get("step"))))
            .add("/orders", p -> new MyOrdersPage(ctx))
            .add("/orders/:id", p -> new TrackOrderPage(ctx, parseId(p.get("id"))))
            .add("/profile", p -> new ProfilePage(ctx))
            // Phase 6B: rider
            .add("/rider", p -> new RiderHomePage(ctx))
            .add("/rider/jobs/:id", p -> new RiderJobPage(ctx, parseId(p.get("id"))))
            // Phase 7: admin
            .add("/admin", p -> new AdminDashboardPage(ctx))
            .add("/admin/orders", p -> new AdminOrdersPage(ctx))
            .add("/admin/pricing", p -> new AdminPricingPage(ctx))
            .add("/admin/users", p -> new AdminUsersPage(ctx))
            .add("/admin/reports", p -> new AdminReportsPage(ctx))
            .fallback("#/signin");
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int parseStep(String step) {
        try {
            return Integer.parseInt(step);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Role tabFromRoute(String role) {
        if ("admin".equals(role)) {
            return Role.ADMIN;
        }
        return "rider".equals(role) ? Role.RIDER : Role.CUSTOMER;
    }

    private App() {
    }

}
