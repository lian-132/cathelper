package com.cathelperawa.owo.app.pan;

import android.content.Context;

import com.cathelperawa.owo.app.cookie.CookieCipher;

public final class Secure {

    private Secure() {}

    public static String enc(Context ctx, String plain) {
        if (plain == null || plain.length() == 0) return "";
        if (plain.startsWith(CookieCipher.PREFIX)) return plain;
        String c = CookieCipher.encrypt(ctx, plain);
        if (c == null || c.length() == 0) return "";
        return CookieCipher.PREFIX + c;
    }

    public static String dec(Context ctx, String stored) {
        if (stored == null || stored.length() == 0) return "";
        if (stored.startsWith(CookieCipher.PREFIX)) {
            String p = CookieCipher.decrypt(ctx, stored.substring(CookieCipher.PREFIX.length()));
            return p == null ? "" : p;
        }

        String p = CookieCipher.decrypt(ctx, stored);
        if (p != null && p.length() > 0) return p;
        return stored;
    }

    public static boolean isEncrypted(Context ctx, String stored) {
        if (stored == null || stored.length() == 0) return false;
        if (stored.startsWith(CookieCipher.PREFIX)) return true;
        String p = CookieCipher.decrypt(ctx, stored);
        return p != null && p.length() > 0;
    }
}
