package com.liskovsoft.youtubeapi.auth.V2;

import android.net.Uri;
import android.os.SystemClock;
import com.liskovsoft.googlecommon.common.models.auth.UserCode;
import com.liskovsoft.googlecommon.common.models.auth.AccessToken;

/** Host fixes to the pinned device flow: server interval, expiry, terminal errors and verification URL. */
public final class OndaDevicePolicy {
    private static volatile long intervalMs=5000;
    private static volatile long expiresAt;
    private static volatile String verificationUrl;
    public static synchronized UserCode observe(UserCode result) {
        if(result == null || result.getDeviceCode() == null || result.getUserCode() == null) throw new IllegalStateException("Missing device authorization");
        Uri uri=Uri.parse(result.getVerificationUrl() == null ? "" : result.getVerificationUrl());
        String host=uri.getHost();
        if(!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null ||
           !(host.equals("google.com") || host.endsWith(".google.com") || host.equals("youtube.com") || host.endsWith(".youtube.com"))) {
            throw new IllegalStateException("Unexpected authorization origin");
        }
        intervalMs=result.getInterval() > 0 ? result.getInterval()*1000L : 5000L;
        if(result.getExpiresIn() <= 0) throw new IllegalStateException("Invalid device-code expiry");
        expiresAt=SystemClock.elapsedRealtime()+result.getExpiresIn()*1000L;
        verificationUrl=uri.toString();
        return result;
    }
    public static String getVerificationUrl() { return verificationUrl; }
    public static void ensureValid() {
        if(expiresAt <= SystemClock.elapsedRealtime()) throw new IllegalStateException("Device authorization expired");
    }
    public static long pollIntervalMs() {
        ensureValid();
        return Math.min(intervalMs,Math.max(1,expiresAt-SystemClock.elapsedRealtime()));
    }
    public static synchronized void checkResponse(AccessToken result) {
        if(result == null || result.getRefreshToken() != null) return;
        String error=result.getError();
        if("slow_down".equals(error)) { intervalMs+=5000; return; }
        if(error == null || "authorization_pending".equals(error)) return;
        throw new IllegalStateException("Device authorization rejected");
    }
}
