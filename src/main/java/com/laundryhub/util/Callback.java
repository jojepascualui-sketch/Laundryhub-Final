package com.laundryhub.util;

import com.laundryhub.exception.ApiException;

/**
 * Result of an asynchronous call (network requests finish later in the browser).
 *
 * @param <T> type of the successful result
 */
public interface Callback<T> {

    /**
     * Called when the call succeeded.
     *
     * @param result the result
     */
    void onSuccess(T result);

    /**
     * Called when the call failed.
     *
     * @param error the error; its message can be shown to the user
     */
    void onError(ApiException error);
}
