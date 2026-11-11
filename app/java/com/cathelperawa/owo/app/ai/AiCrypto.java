package com.cathelperawa.owo.app.ai;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.cathelperawa.owo.app.core.HookLog;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AiCrypto {

    private AiCrypto() {}

    private static final String PROVIDER = "AndroidKeyStore";
    private static final String ALIAS = "cathelper_ai_provider_key";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    public static String encrypt(String plain) {
        if (plain == null || plain.length() == 0) return "";
        try {
            SecretKey key = getOrCreateKey();
            if (key == null) return "";
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes("UTF-8"));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Throwable t) {
            HookLog.e("AI Key 加密失败", t);
            return "";
        }
    }

    public static String decrypt(String b64) {
        if (b64 == null || b64.length() == 0) return "";
        try {
            byte[] all = Base64.decode(b64, Base64.NO_WRAP);
            if (all.length <= IV_LEN) return "";
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);

            SecretKey key = getOrCreateKey();
            if (key == null) return "";
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), "UTF-8");
        } catch (Throwable t) {
            
            return "";
        }
    }

    private static SecretKey getOrCreateKey() {
        try {
            KeyStore ks = KeyStore.getInstance(PROVIDER);
            ks.load(null);
            if (ks.containsAlias(ALIAS)) {
                java.security.Key k = ks.getKey(ALIAS, null);
                if (k instanceof SecretKey) return (SecretKey) k;
            }
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return kg.generateKey();
        } catch (Throwable t) {
            HookLog.e("Keystore 密钥创建失败", t);
            return null;
        }
    }
}
