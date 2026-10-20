package com.laundryhub.exception;

/**
 * An error returned by Supabase (REST, Auth or Storage) or a network failure.
 * {@link #getMessage()} is safe to show to the user.
 */
public class ApiException extends LaundryHubException {

    /** HTTP status used when the request never reached the server. */
    public static final int NETWORK_ERROR = 0;

    private final int status;
    private final String code;

    /**
     * @param status  HTTP status, or {@link #NETWORK_ERROR}
     * @param code    error code from the server (e.g. {@code PT409}), may be empty
     * @param message user-readable message
     */
    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code == null ? "" : code;
    }

    /** @return HTTP status, or {@link #NETWORK_ERROR} */
    public int getStatus() {
        return status;
    }

    /** @return server error code, never null */
    public String getCode() {
        return code;
    }

    /** @return true when the user must sign in again */
    public boolean isUnauthorized() {
        return status == 401;
    }

    /** @return true when another user changed the same data first (e.g. booking taken) */
    public boolean isConflict() {
        return status == 409;
    }
}
