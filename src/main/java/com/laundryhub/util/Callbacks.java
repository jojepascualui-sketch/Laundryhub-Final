package com.laundryhub.util;

import com.laundryhub.exception.ApiException;
import com.laundryhub.exception.LaundryHubException;
import java.util.function.Function;

/**
 * Helpers for chaining asynchronous callbacks.
 */
public final class Callbacks {

    private static final int BAD_DATA = 500;

    private Callbacks() {
    }

    /**
     * Converts a successful result before passing it on; errors pass through unchanged.
     * A conversion error (e.g. bad data) is reported as an {@link ApiException}.
     *
     * @param target    the final callback
     * @param converter converts the raw result
     * @param <S>       raw result type
     * @param <T>       converted type
     * @return a callback for the raw result
     */
    public static <S, T> Callback<S> map(Callback<T> target, Function<S, T> converter) {
        return new Callback<S>() {
            @Override
            public void onSuccess(S result) {
                T converted;
                try {
                    converted = converter.apply(result);
                } catch (LaundryHubException e) {
                    target.onError(new ApiException(BAD_DATA, "", e.getMessage()));
                    return;
                }
                target.onSuccess(converted);
            }

            @Override
            public void onError(ApiException error) {
                target.onError(error);
            }
        };
    }

    /**
     * @param target callback that only needs to know the call finished
     * @param <S>    ignored result type
     * @return a callback that drops the result
     */
    public static <S> Callback<S> ignoreResult(Callback<Void> target) {
        return map(target, ignored -> null);
    }
}
