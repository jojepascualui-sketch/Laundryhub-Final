package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.LaundryHubException;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Callbacks;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link CatalogRepository} backed by Supabase.
 */
public final class SupabaseCatalogRepository implements CatalogRepository {

    private static final int SETTINGS_ROW_ID = 1;
    private static final String PUBLIC_BUCKET = "shop-public";

    private final SupabaseClient client;

    /**
     * @param client Supabase client
     */
    public SupabaseCatalogRepository(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void loadSettings(Callback<ShopSettings> callback) {
        client.selectOne("shop_settings", Query.select("*").eq("id", SETTINGS_ROW_ID), Callbacks.map(callback, row -> {
            if (row == null) {
                throw new LaundryHubException(
                        "Shop settings are missing. Run supabase/migrations/0009 in the SQL Editor.");
            }
            return ModelMapper.toSettings(row);
        }));
    }

    @Override
    public void saveSettings(ShopSettings settings, Callback<Void> callback) {
        client.update("shop_settings", Query.filter().eq("id", SETTINGS_ROW_ID), ModelMapper.fromSettings(settings),
                new Callback<JsonArray>() {
                    @Override
                    public void onSuccess(JsonArray rows) {
                        if (rows.isEmpty()) {
                            onError(new ApiException(403, "", "Only the admin can change prices."));
                        } else {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                });
    }

    @Override
    public void uploadGcashQr(Object browserFile, String contentType, Callback<String> callback) {
        String ext = contentType.endsWith("png") ? "png" : contentType.endsWith("webp") ? "webp" : "jpg";
        // A new name each time, so phones never show an old cached QR.
        String path = "gcash-qr-" + System.currentTimeMillis() + "." + ext;
        client.upload(PUBLIC_BUCKET, path, browserFile, contentType, new Callback<String>() {
            @Override
            public void onSuccess(String saved) {
                setQrPath(saved, Callbacks.map(callback, ignored -> saved));
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void removeGcashQr(Callback<Void> callback) {
        setQrPath(null, callback);
    }

    @Override
    public String gcashQrUrl(String path) {
        return client.publicUrl(PUBLIC_BUCKET, path);
    }

    private void setQrPath(String path, Callback<Void> callback) {
        client.update("shop_settings", Query.filter().eq("id", SETTINGS_ROW_ID), new JsonObject().put("gcash_qr_path", path),
                new Callback<JsonArray>() {
                    @Override
                    public void onSuccess(JsonArray rows) {
                        if (rows.isEmpty()) {
                            onError(new ApiException(403, "", "Only the admin can change the GCash QR."));
                        } else {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                });
    }

    @Override
    public void findAvailableSoaps(Callback<List<Soap>> callback) {
        client.select("soaps", Query.select("*").eq("available", true).order("sort_order", false).order("id", false),
                Callbacks.map(callback, SupabaseCatalogRepository::toSoaps));
    }

    @Override
    public void findAllSoaps(Callback<List<Soap>> callback) {
        client.select("soaps", Query.select("*").order("sort_order", false).order("id", false),
                Callbacks.map(callback, SupabaseCatalogRepository::toSoaps));
    }

    @Override
    public void saveSoap(Soap soap, Callback<Soap> callback) {
        JsonObject row = new JsonObject()
                .put("name", soap.getName())
                .put("description", soap.getDescription())
                .put("add_on_per_kg", MoneyFormatter.toPesos(soap.getAddOnPerKg()))
                .put("available", soap.isAvailable());
        Callback<JsonArray> done = Callbacks.map(callback, rows -> ModelMapper.toSoap(rows.getObject(0)));
        if (soap.getId() == 0) {
            client.insert("soaps", row, done);
        } else {
            client.update("soaps", Query.filter().eq("id", soap.getId()), row, done);
        }
    }

    private static List<Soap> toSoaps(JsonArray rows) {
        List<Soap> soaps = new ArrayList<>();
        for (JsonObject row : rows.objects()) {
            soaps.add(ModelMapper.toSoap(row));
        }
        return soaps;
    }
}
