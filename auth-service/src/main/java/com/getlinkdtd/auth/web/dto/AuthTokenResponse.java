package com.getlinkdtd.auth.web.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AccountResponse account) {
    @Override
    public String toString() {
        return "AuthTokenResponse[accessToken=<redacted>, tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", account=" + account + "]";
    }
}
