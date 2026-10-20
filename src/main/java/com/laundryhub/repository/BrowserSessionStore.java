package com.laundryhub.repository;

import org.teavm.jso.browser.Storage;

/**
 * Keeps the session in localStorage ("Remember me") or sessionStorage (until the tab closes).
 * Storage can be blocked (private mode), so every access is guarded.
 */
public final class BrowserSessionStore implements SessionStore {

    private static final String KEY = "laundryhub.session";

    @Override
    public String load() {
        String v = read(Storage.getLocalStorage());
        return v != null ? v : read(Storage.getSessionStorage());
    }

    @Override
    public void save(String sessionJson, boolean remember) {
        clear();
        try {
            (remember ? Storage.getLocalStorage() : Storage.getSessionStorage()).setItem(KEY, sessionJson);
        } catch (RuntimeException blocked) {
            // Storage unavailable: the session lasts until the page reloads.
        }
    }

    @Override
    public void clear() {
        remove(Storage.getLocalStorage());
        remove(Storage.getSessionStorage());
    }

    private static String read(Storage storage) {
        try {
            return storage == null ? null : storage.getItem(KEY);
        } catch (RuntimeException blocked) {
            return null;
        }
    }

    private static void remove(Storage storage) {
        try {
            if (storage != null) {
                storage.removeItem(KEY);
            }
        } catch (RuntimeException blocked) {
            // ignore
        }
    }
}
