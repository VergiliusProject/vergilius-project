package com.vergiliusproject.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cache policy constants and scalar-key ETag computation. ETags hash only
 * scalar inputs (never response bodies) so they stay stable regardless of
 * JSON serialization order.
 */
public final class ApiCache {

    public static final String BUILD_SCOPED =
            "public, max-age=86400, stale-while-revalidate=604800";
    public static final String VOLATILE = "public, max-age=300";

    private ApiCache() {}

    public static String etag(Object... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Object part : parts) {
                md.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0x1f);
            }
            return "\"" + HexFormat.of().formatHex(md.digest(), 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String trimmed = candidate.trim();
            if ("*".equals(trimmed) || trimmed.equals(etag)) {
                return true;
            }
        }
        return false;
    }
}
