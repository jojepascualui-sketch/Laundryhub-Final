package com.laundryhub.repository;

import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.util.Callback;
import java.util.List;

/**
 * Shop settings and fabric soaps (tables shop_settings and soaps). Everyone can read;
 * only ADMIN can write (enforced by RLS).
 */
public interface CatalogRepository {

    /** @param callback receives the shop settings */
    void loadSettings(Callback<ShopSettings> callback);

    /**
     * @param settings new settings
     * @param callback called when saved
     */
    void saveSettings(ShopSettings settings, Callback<Void> callback);

    /** @param callback receives soaps customers can choose, in display order */
    void findAvailableSoaps(Callback<List<Soap>> callback);

    /** @param callback receives every soap (admin) */
    void findAllSoaps(Callback<List<Soap>> callback);

    /**
     * Inserts a new soap (id 0) or updates an existing one.
     *
     * @param soap     the soap
     * @param callback receives the saved soap
     */
    void saveSoap(Soap soap, Callback<Soap> callback);
}
