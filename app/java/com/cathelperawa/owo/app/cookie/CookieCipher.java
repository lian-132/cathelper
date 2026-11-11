package com.cathelperawa.owo.app.cookie;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class CookieCipher {

    private CookieCipher() {}

    public static final String PREFIX = "v1:";

    private static final String APP = "com.cathelperawa.owo.app";
    private static final String SALT = "cathelper.cookie.v1";

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    public static String encrypt(Context ctx, String plain) {
        if (ctx == null || plain == null) return "";
        try {
            SecretKeySpec key = keyOf(ctx);
            if (key == null) return "";
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes("UTF-8"));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Throwable t) {
            return "";
        }
    }

    public static String decrypt(Context ctx, String b64) {
        if (ctx == null || b64 == null || b64.length() == 0) return "";
        try {
            SecretKeySpec key = keyOf(ctx);
            if (key == null) return "";
            byte[] all = Base64.decode(b64, Base64.NO_WRAP);
            if (all.length <= IV_LEN) return "";
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = c.doFinal(all, IV_LEN, all.length - IV_LEN);
            return new String(pt, "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    private static volatile SecretKeySpec cachedKey;

    private static SecretKeySpec keyOf(Context ctx) {
        SecretKeySpec k = cachedKey;
        if (k != null) return k;
        try {
            String id = Settings.Secure.getString(
                    ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null) id = "";
            String material = id + "|" + APP + "|" + SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] kk = md.digest(material.getBytes("UTF-8"));
            k = new SecretKeySpec(kk, "AES");
            cachedKey = k;
            return k;
        } catch (Throwable t) {
            return null;
        }
    }
}
