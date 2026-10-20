package com.laundryhub.web;

import org.teavm.jso.JSBody;

/**
 * Reads the public keys from {@code window.LH_CONFIG} (web-dist/config.js, generated from
 * config.properties at build time).
 */
public final class BrowserConfig {

    private BrowserConfig() {
    }

    /** @return Supabase project URL, or "" when not set */
    public static String supabaseUrl() {
        return get("SUPABASE_URL");
    }

    /** @return Supabase publishable key, or "" when not set */
    public static String supabaseKey() {
        return get("SUPABASE_PUBLISHABLE_KEY");
    }

    /** @return Google Maps key, or "" when not set */
    public static String googleMapsKey() {
        return get("GOOGLE_MAPS_KEY");
    }

    /** @return true when both Supabase values are present */
    public static boolean isSupabaseConfigured() {
        return !supabaseUrl().isEmpty() && !supabaseKey().isEmpty();
    }

    @JSBody(params = "name", script = "var lhC = window.LH_CONFIG; return (lhC && lhC[name]) ? String(lhC[name]) : '';")
    private static native String get(String name);
}
