package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TimWallet {

    private TimWallet() {}

    

    private static Class<?> CLS_ENCRYPT_REQ;
    private static Object encryptReq;
    private static Method M_ENCRYPT;    
    private static Method M_DECRYPT;    

    private static void ensureEncryptRequest() {
        try {
            if (CLS_ENCRYPT_REQ == null) {
                ClassLoader cl = QQEnv.classLoader();
                CLS_ENCRYPT_REQ = findClass(cl, "com.tenpay.sdk.basebl.EncryptRequest");
                if (CLS_ENCRYPT_REQ != null) {
                    
                    M_ENCRYPT = findMethod(CLS_ENCRYPT_REQ, "encypt", 6);
                    if (M_ENCRYPT == null) M_ENCRYPT = findMethod(CLS_ENCRYPT_REQ, "encrypt", 6);
                    M_DECRYPT = findMethod(CLS_ENCRYPT_REQ, "decypt", 4);
                    if (M_DECRYPT == null) M_DECRYPT = findMethod(CLS_ENCRYPT_REQ, "decrypt", 4);
                    HookLog.i("TimWallet.EncryptRequest: cls=" + (CLS_ENCRYPT_REQ != null)
                            + " enc=" + (M_ENCRYPT != null) + " dec=" + (M_DECRYPT != null));
                }
            }
        } catch (Throwable t) {
            HookLog.e("ensureEncryptRequest 失败", t);
        }
    }

    private static Object encryptRequest() {
        ensureEncryptRequest();
        if (CLS_ENCRYPT_REQ == null) return null;
        try {
            if (encryptReq == null) {
                
                encryptReq = newInstance(CLS_ENCRYPT_REQ, QQEnv.appContext());
                if (encryptReq == null) encryptReq = newInstance(CLS_ENCRYPT_REQ);
            }
            return encryptReq;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void prewarm() {
        try {
            ensureEncryptRequest();
            Object req = encryptRequest();
            HookLog.i("TimWallet 预热: cls=" + (CLS_ENCRYPT_REQ != null)
                    + " enc=" + (M_ENCRYPT != null) + " req=" + (req != null));
        } catch (Throwable t) { HookLog.w("TimWallet 预热失败: " + t); }
    }

    public static String encrypt(String uin, String salt, int randomInt,
                                 String formString, String psKey, String hexGuid) {
        Object req = encryptRequest();
        if (req == null || M_ENCRYPT == null) return null;
        if (uin == null || psKey == null || hexGuid == null) {
            HookLog.w("encrypt: 缺参 uin/psKey/hexGuid");
            return null;
        }
        try {
            M_ENCRYPT.setAccessible(true);
            Object ret = M_ENCRYPT.invoke(req, uin, salt, Integer.valueOf(randomInt),
                    formString, psKey, hexGuid);
            if (ret == null) return null;
            String encText = strField(ret, "encText");
            if (encText != null) return encText;
            
            if (ret instanceof String) return (String) ret;
            return null;
        } catch (Throwable t) {
            HookLog.e("encrypt 失败", t);
            return null;
        }
    }

    public static String decrypt(String uin, String cgiName, int randomInt, String encStr) {
        Object req = encryptRequest();
        if (req == null || M_DECRYPT == null) return null;
        if (uin == null || encStr == null) return null;
        try {
            M_DECRYPT.setAccessible(true);
            Object ret = M_DECRYPT.invoke(req, uin, cgiName, Integer.valueOf(randomInt), encStr);
            if (ret == null) return null;
            String s = strField(ret, "decryptStr");
            if (s != null) return s;
            if (ret instanceof String) return (String) ret;
            return null;
        } catch (Throwable t) {
            HookLog.e("decrypt 失败", t);
            return null;
        }
    }

    

    private static Class<?> CLS_PSKEY_PROC;
    private static Class<?> CLS_SESSION_KEY;
    private static Class<?> CLS_ENC_PROC;
    private static Method M_GET_PSKEY;      
    private static Method M_PROCESS_ENC;    

    private static void ensureProcessor() {
        try {
            if (CLS_ENC_PROC == null) {
                ClassLoader cl = QQEnv.classLoader();
                CLS_PSKEY_PROC = findClass(cl, "com.tenpay.sdk.net.core.processor.PsKeyProcessor");
                CLS_SESSION_KEY = findClass(cl, "com.tenpay.sdk.net.core.comm.SessionKey");
                CLS_ENC_PROC = findClass(cl, "com.tenpay.sdk.net.core.processor.EncryptProcessor");
                if (CLS_PSKEY_PROC != null) M_GET_PSKEY = findMethod(CLS_PSKEY_PROC, "getPsKey", 1);
                if (CLS_ENC_PROC != null) M_PROCESS_ENC = findMethod(CLS_ENC_PROC, "processEncrypt", 6);
                HookLog.i("TimWallet.Processor: psKey=" + (CLS_PSKEY_PROC != null)
                        + " sessionKey=" + (CLS_SESSION_KEY != null)
                        + " encProc=" + (CLS_ENC_PROC != null)
                        + " getPsKey=" + (M_GET_PSKEY != null)
                        + " processEncrypt=" + (M_PROCESS_ENC != null));
            }
        } catch (Throwable t) {
            HookLog.e("ensureProcessor 失败", t);
        }
    }

    public static Map<String, String> walletEncrypt(String url, String secCmd, Map<String, String> form) {
        ensureProcessor();
        if (CLS_ENC_PROC == null || M_PROCESS_ENC == null) return null;
        if (url == null || secCmd == null || form == null) return null;
        String uin = QQEnv.currentUin();
        if (uin == null) return null;
        try {
            
            Object psKey = null;
            if (CLS_PSKEY_PROC != null && M_GET_PSKEY != null) {
                Object psp = newInstance(CLS_PSKEY_PROC);
                if (psp != null) {
                    M_GET_PSKEY.setAccessible(true);
                    psKey = M_GET_PSKEY.invoke(psp, url);
                }
            }

            Object sessionKey = null;
            if (CLS_SESSION_KEY != null) {
                sessionKey = newInstance(CLS_SESSION_KEY, secCmd, psKey, uin);
            }
            if (sessionKey == null) {
                HookLog.w("walletEncrypt: SessionKey 构造失败");
                return null;
            }

            Object proc = newInstance(CLS_ENC_PROC);
            if (proc == null) return null;
            M_PROCESS_ENC.setAccessible(true);
            Object ret = M_PROCESS_ENC.invoke(proc, url, sessionKey, form,
                    Boolean.TRUE, Boolean.TRUE, null);
            if (ret == null) return null;

            Object body = objField(ret, "bodyData");
            if (body instanceof Map) {
                Map<String, String> out = new LinkedHashMap<String, String>();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) body).entrySet()) {
                    if (e.getKey() == null) continue;
                    out.put(String.valueOf(e.getKey()),
                            e.getValue() == null ? null : String.valueOf(e.getValue()));
                }
                return out;
            }
            if (ret instanceof Map) {
                Map<String, String> out = new LinkedHashMap<String, String>();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) ret).entrySet()) {
                    if (e.getKey() == null) continue;
                    out.put(String.valueOf(e.getKey()),
                            e.getValue() == null ? null : String.valueOf(e.getValue()));
                }
                return out;
            }
            return null;
        } catch (Throwable t) {
            HookLog.e("walletEncrypt 失败", t);
            return null;
        }
    }

    public static String walletPsKey(String url) {
        ensureProcessor();
        if (CLS_PSKEY_PROC == null || M_GET_PSKEY == null || url == null) return null;
        try {
            Object psp = newInstance(CLS_PSKEY_PROC);
            if (psp == null) return null;
            M_GET_PSKEY.setAccessible(true);
            Object v = M_GET_PSKEY.invoke(psp, url);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    

    
    public static String formString(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String v = e.getValue();
            if (v == null) continue;                 
            if (e.getKey() == null) continue;
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey());
            sb.append("=");
            try {
                sb.append(URLEncoder.encode(v, "UTF-8"));
            } catch (Throwable t) {
                sb.append(v);
            }
        }
        return sb.toString();
    }

    public static byte[] add4Byte(byte[] body) {
        if (body == null) return null;
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
            dos.writeInt(body.length + 4);
            dos.write(body);
            dos.flush();
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    

    private static String strField(Object o, String field) {
        Object v = objField(o, field);
        return v == null ? null : String.valueOf(v);
    }

    private static Object objField(Object o, String field) {
        if (o == null) return null;
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(o);
            } catch (Throwable t) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static Class<?> findClass(ClassLoader cl, String name) {
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable ignore) {
        }
        try {
            return de.robv.android.xposed.XposedHelpers.findClassIfExists(name, cl);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String name, int argc) {
        if (cls == null) return null;
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == argc) return m;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Object newInstance(Class<?> cls, Object... args) {
        if (cls == null) return null;
        Object[] a = (args == null) ? new Object[0] : args;
        for (java.lang.reflect.Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterTypes().length != a.length) continue;
            try {
                c.setAccessible(true);
                return c.newInstance(a);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
