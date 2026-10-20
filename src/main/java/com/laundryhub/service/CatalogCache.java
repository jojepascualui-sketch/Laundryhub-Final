package com.laundryhub.service;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.repository.CatalogRepository;
import com.laundryhub.util.Callback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads shop settings and available soaps once and shares them between pages.
 * Callers that ask while a load is running wait for the same result.
 */
public final class CatalogCache {

    /** Settings plus the soaps customers can choose. */
    public static final class Catalog {
        private final ShopSettings settings;
        private final List<Soap> soaps;

        Catalog(ShopSettings settings, List<Soap> soaps) {
            this.settings = settings;
            this.soaps = Collections.unmodifiableList(new ArrayList<>(soaps));
        }

        /** @return shop settings */
        public ShopSettings getSettings() {
            return settings;
        }

        /** @return available soaps in display order */
        public List<Soap> getSoaps() {
            return soaps;
        }

        /**
         * @param id soap id
         * @return the soap, or null
         */
        public Soap findSoap(long id) {
            for (Soap s : soaps) {
                if (s.getId() == id) {
                    return s;
                }
            }
            return null;
        }
    }

    private final CatalogRepository repository;
    private final List<Callback<Catalog>> waiting = new ArrayList<>();
    private Catalog catalog;
    private boolean loading;

    /**
     * @param repository settings/soaps repository
     */
    public CatalogCache(CatalogRepository repository) {
        this.repository = repository;
    }

    /** @return the loaded catalog, or null before the first load finished */
    public Catalog getLoaded() {
        return catalog;
    }

    /** Forgets the cached data (e.g. after the admin changes prices). */
    public void invalidate() {
        catalog = null;
    }

    /**
     * @param callback receives the catalog (immediately when already loaded)
     */
    public void get(Callback<Catalog> callback) {
        if (catalog != null) {
            callback.onSuccess(catalog);
            return;
        }
        waiting.add(callback);
        if (loading) {
            return;
        }
        loading = true;
        repository.loadSettings(new Callback<ShopSettings>() {
            @Override
            public void onSuccess(ShopSettings settings) {
                repository.findAvailableSoaps(new Callback<List<Soap>>() {
                    @Override
                    public void onSuccess(List<Soap> soaps) {
                        catalog = new Catalog(settings, soaps);
                        finish(null);
                    }

                    @Override
                    public void onError(ApiException error) {
                        finish(error);
                    }
                });
            }

            @Override
            public void onError(ApiException error) {
                finish(error);
            }
        });
    }

    private void finish(ApiException error) {
        loading = false;
        List<Callback<Catalog>> callbacks = new ArrayList<>(waiting);
        waiting.clear();
        for (Callback<Catalog> cb : callbacks) {
            if (error == null) {
                cb.onSuccess(catalog);
            } else {
                cb.onError(error);
            }
        }
    }
}
