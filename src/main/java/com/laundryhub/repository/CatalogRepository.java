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

    /**
     * Uploads the shop's GCash QR Ph image and saves its path in shop_settings (ADMIN only).
     *
     * @param browserFile image File from an input element
     * @param contentType e.g. image/png
     * @param callback    receives the new storage path
     */
    void uploadGcashQr(Object browserFile, String contentType, Callback<String> callback);

    /** @param callback called when the QR was removed from the shop settings (ADMIN only) */
    void removeGcashQr(Callback<Void> callback);

    /**
     * @param path storage path from {@link ShopSettings#getGcashQrPath()}
     * @return public image address
     */
    String gcashQrUrl(String path);

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
