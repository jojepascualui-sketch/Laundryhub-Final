package com.laundryhub.web;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.User;
import com.laundryhub.repository.AdminRepository;
import com.laundryhub.repository.BrowserSessionStore;
import com.laundryhub.repository.CatalogRepository;
import com.laundryhub.repository.OrderRepository;
import com.laundryhub.repository.ProfileRepository;
import com.laundryhub.repository.RiderJobRepository;
import com.laundryhub.repository.SupabaseAdminRepository;
import com.laundryhub.repository.SupabaseCatalogRepository;
import com.laundryhub.repository.SupabaseClient;
import com.laundryhub.repository.SupabaseOrderRepository;
import com.laundryhub.repository.SupabaseProfileRepository;
import com.laundryhub.repository.SupabaseRiderJobRepository;
import com.laundryhub.repository.XhrHttpClient;
import com.laundryhub.model.Order;
import com.laundryhub.service.AuthService;
import com.laundryhub.service.CatalogCache;
import com.laundryhub.service.OrderDraft;
import com.laundryhub.ui.Router;
import com.laundryhub.util.Callback;

/**
 * Shared objects for every page: the Supabase client, repositories, router and signed-in user.
 * Pages receive it in their constructor (no global state inside pages).
 */
public final class AppContext {

    private final SupabaseClient client;
    private final ProfileRepository profiles;
    private final CatalogRepository catalog;
    private final OrderRepository orders;
    private final RiderJobRepository riderJobs;
    private final AdminRepository admin;
    private final AuthService auth;
    private final CatalogCache catalogCache;
    private final OrderDraft orderDraft = new OrderDraft();
    private Order lastPlacedOrder;
    private String adminSearch = "";
    private long adminSelectedOrderId = -1;
    private final String configError;
    private Router router;
    private User currentUser;

    private AppContext(SupabaseClient client, String configError) {
        this.client = client;
        this.configError = configError;
        if (client != null) {
            profiles = new SupabaseProfileRepository(client);
            catalog = new SupabaseCatalogRepository(client);
            orders = new SupabaseOrderRepository(client);
            riderJobs = new SupabaseRiderJobRepository(client);
            admin = new SupabaseAdminRepository(client);
            auth = new AuthService(client, profiles);
            catalogCache = new CatalogCache(catalog);
        } else {
            profiles = null;
            catalog = null;
            orders = null;
            riderJobs = null;
            admin = null;
            auth = null;
            catalogCache = null;
        }
    }

    /** @return a context wired to Supabase, or one that reports missing configuration */
    public static AppContext create() {
        if (!BrowserConfig.isSupabaseConfigured()) {
            return new AppContext(null,
                    "Supabase is not connected yet. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to config.properties and rebuild.");
        }
        try {
            SupabaseClient c = new SupabaseClient(BrowserConfig.supabaseUrl(), BrowserConfig.supabaseKey(),
                    new XhrHttpClient(), new BrowserSessionStore());
            return new AppContext(c, null);
        } catch (RuntimeException e) {
            return new AppContext(null, e.getMessage());
        }
    }

    /** @return true when Supabase is configured */
    public boolean isConfigured() {
        return client != null;
    }

    /** @return why Supabase is unavailable, or null */
    public String getConfigError() {
        return configError;
    }

    /** @return Supabase client (null when not configured) */
    public SupabaseClient getClient() {
        return client;
    }

    /** @return profile repository */
    public ProfileRepository getProfiles() {
        return profiles;
    }

    /** @return settings and soaps repository */
    public CatalogRepository getCatalog() {
        return catalog;
    }

    /** @return order repository */
    public OrderRepository getOrders() {
        return orders;
    }

    /** @return rider job repository */
    public RiderJobRepository getRiderJobs() {
        return riderJobs;
    }

    /** @return admin repository */
    public AdminRepository getAdmin() {
        return admin;
    }

    /** @return sign in / sign up / sign out (null when not configured) */
    public AuthService getAuth() {
        return auth;
    }

    /** @return shared shop settings + soaps (null when not configured) */
    public CatalogCache getCatalogCache() {
        return catalogCache;
    }

    /** @return the order being created in the wizard */
    public OrderDraft getOrderDraft() {
        return orderDraft;
    }

    /** @return the order just placed (for the "Order placed!" screen), or null */
    public Order getLastPlacedOrder() {
        return lastPlacedOrder;
    }

    /** @param order the order just placed */
    public void setLastPlacedOrder(Order order) {
        lastPlacedOrder = order;
    }

    /** @return text typed in the admin search box (applied on the Orders page) */
    public String getAdminSearch() {
        return adminSearch;
    }

    /** @param text admin search text */
    public void setAdminSearch(String text) {
        adminSearch = text == null ? "" : text;
    }

    /** @return order to open on the admin Orders page, or -1 */
    public long getAdminSelectedOrderId() {
        return adminSelectedOrderId;
    }

    /** @param orderId order to open on the admin Orders page, or -1 */
    public void setAdminSelectedOrderId(long orderId) {
        adminSelectedOrderId = orderId;
    }

    /** @return the router */
    public Router getRouter() {
        return router;
    }

    void setRouter(Router value) {
        router = value;
    }

    /** @return signed-in user, or null */
    public User getCurrentUser() {
        return currentUser;
    }

    /** @param user signed-in user, or null after sign out */
    public void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Signs out and goes to the sign-in page.
     */
    public void signOut() {
        currentUser = null;
        orderDraft.reset();
        lastPlacedOrder = null;
        if (auth == null) {
            navigate("#/signin");
            return;
        }
        auth.signOut(new Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                navigate("#/signin");
            }

            @Override
            public void onError(ApiException error) {
                navigate("#/signin");
            }
        });
    }

    /**
     * @param route hash route, e.g. "#/home"
     */
    public void navigate(String route) {
        router.navigate(route);
    }
}
