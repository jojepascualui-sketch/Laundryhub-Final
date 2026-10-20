package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;

/**
 * A Supabase login session (access token + refresh token).
 */
public final class AuthSession {

    /** Refresh this many seconds before the access token expires. */
    public static final long REFRESH_MARGIN_SECONDS = 60;

    private final String accessToken;
    private final String refreshToken;
    private final long expiresAt;
    private final String userId;
    private final String email;

    /**
     * @param accessToken  JWT sent as "Authorization: Bearer"
     * @param refreshToken token used to get a new access token
     * @param expiresAt    expiry time in epoch seconds
     * @param userId       auth user id
     * @param email        e-mail, may be null
     */
    public AuthSession(String accessToken, String refreshToken, long expiresAt, String userId, String email) {
        if (accessToken == null || accessToken.isEmpty() || refreshToken == null || refreshToken.isEmpty()) {
            throw new ValidationException("The login session is incomplete.");
        }
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.email = email;
    }

    /** @return access token */
    public String getAccessToken() {
        return accessToken;
    }

    /** @return refresh token */
    public String getRefreshToken() {
        return refreshToken;
    }

    /** @return expiry in epoch seconds */
    public long getExpiresAt() {
        return expiresAt;
    }

    /** @return auth user id */
    public String getUserId() {
        return userId;
    }

    /** @return e-mail, may be null */
    public String getEmail() {
        return email;
    }

    /**
     * @param nowEpochSeconds current time
     * @return true when the access token should be refreshed before use
     */
    public boolean needsRefresh(long nowEpochSeconds) {
        return nowEpochSeconds >= expiresAt - REFRESH_MARGIN_SECONDS;
    }
}
