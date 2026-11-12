package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Host;
import com.cathelperawa.owo.app.feature.FeatureMsgTime;
import de.robv.android.xposed.XC_MethodHook;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class QqDbKey {
    private static volatile byte[] KEY;
    private static volatile boolean installed = false;

    private QqDbKey() {
    }

    public static void install(ClassLoader classLoader) {
        boolean z;
        if (installed) {
            return;
        }
        try {
            Class<?> findClass = HookApi.findClass(classLoader, "com.tencent.mobileqq.app.SQLiteDatabase", "com.tencent.wcdb.database.SQLiteDatabase", "com.tencent.wcdb.database.SQLiteConnection", "com.tencent.wcdb.core.Database");
            if (findClass == null) {
                probe(classLoader, "主类不存在");
                return;
            }
            probe(classLoader, "主类=" + findClass.getName());
            int i = 0;
            for (Method method : findClass.getDeclaredMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                int length = parameterTypes.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        z = false;
                        break;
                    } else {
                        if (parameterTypes[i2] == byte[].class) {
                            z = true;
                            break;
                        }
                        i2++;
                    }
                }
                if (z) {
                    try {
                        method.setAccessible(true);
                        HookApi.before(method, new HookApi.Action() { 
                            @Override 
                            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                try {
                                    QqDbKey.grab(methodHookParam.args);
                                } catch (Throwable th) {
                                }
                            }
                        });
                        i++;
                    } catch (Throwable th) {
                    }
                }
            }
            try {
                Class<?> findClass2 = HookApi.findClass(classLoader, "com.tencent.mobileqq.app.SQLiteDatabase", "com.tencent.wcdb.database.SQLiteConnection", "com.tencent.wcdb.database.SQLiteConnectionPool");
                if (findClass2 != null) {
                    for (Method method2 : findClass2.getDeclaredMethods()) {
                        if (method2.getName().toLowerCase().contains("open")) {
                            try {
                                method2.setAccessible(true);
                                HookApi.before(method2, new HookApi.Action() { 
                                    @Override 
                                    public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                        try {
                                            QqDbKey.grabDeep(methodHookParam.args, 0);
                                            QqDbKey.grabDeep(methodHookParam.thisObject, 0);
                                        } catch (Throwable th2) {
                                        }
                                    }
                                });
                                i++;
                            } catch (Throwable th2) {
                            }
                        }
                    }
                    note("conn=" + findClass2.getName() + " 挂了 " + i);
                } else {
                    note("SQLiteConnection 不存在");
                }
            } catch (Throwable th3) {
                note("conn hook 失败 " + th3);
            }
            installed = i > 0;
            note("hooked=" + i);
            HookLog.i("QqDbKey: 挂了 " + i + " 个 open* 方法");
        } catch (Throwable th4) {
            HookLog.w("QqDbKey 安装失败 " + th4);
        }
    }

    private static void probe(ClassLoader classLoader, String str) {
        StringBuilder append = new StringBuilder(str).append("\n");
        String[] strArr = {"com.tencent.mobileqq.app.SQLiteDatabase", "com.tencent.wcdb.database.SQLiteDatabase", "com.tencent.wcdb.database.SQLiteConnection", "com.tencent.wcdb.database.SQLiteCipherSpec", "com.tencent.wcdb.core.Database"};
        for (int i = 0; i < 5; i++) {
            String str2 = strArr[i];
            Class<?> findClass = HookApi.findClass(classLoader, str2);
            append.append(str2).append(" → ").append(findClass != null).append('\n');
            if (findClass != null) {
                Method[] declaredMethods = findClass.getDeclaredMethods();
                int length = declaredMethods.length;
                int i2 = 0;
                int i3 = 0;
                while (i2 < length) {
                    Method method = declaredMethods[i2];
                    int i4 = i3 + 1;
                    if (i3 > 30) {
                        break;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Class<?> cls : method.getParameterTypes()) {
                        sb.append(cls.getSimpleName()).append(',');
                    }
                    append.append("   ").append(method.getName()).append('(').append((CharSequence) sb).append(") : ").append(method.getReturnType().getSimpleName()).append('\n');
                    i2++;
                    i3 = i4;
                }
            }
        }
        note(append.toString());
    }

    public static void grab(Object[] objArr) {
        if (objArr == null || KEY != null) {
            return;
        }
        for (Object obj : objArr) {
            if (obj instanceof byte[]) {
                byte[] bArr = (byte[]) obj;
                if (bArr.length >= 16 && bArr.length <= 64) {
                    KEY = (byte[]) bArr.clone();
                    note("key len=" + bArr.length + " hex=" + hex(bArr));
                    HookLog.i("QqDbKey: 已捕获密钥 len=" + bArr.length);
                }
            }
        }
    }

    public static void grabDeep(Object obj, int i) {
        if (obj == null || KEY != null || i > 3) {
            return;
        }
        if (obj instanceof byte[]) {
            byte[] bArr = (byte[]) obj;
            if (bArr.length >= 16 && bArr.length <= 64) {
                KEY = (byte[]) bArr.clone();
                note("key(deep) len=" + bArr.length + " hex=" + hex(bArr));
                return;
            }
            return;
        }
        if (obj instanceof Object[]) {
            for (Object obj2 : (Object[]) obj) {
                grabDeep(obj2, i + 1);
            }
            return;
        }
        if (obj instanceof List) {
            Iterator it = ((List) obj).iterator();
            while (it.hasNext()) {
                grabDeep(it.next(), i + 1);
            }
            return;
        }
        Class<?> cls = obj.getClass();
        if (cls.getName().startsWith("java.")) {
            return;
        }
        int i2 = 0;
        while (cls != null && cls != Object.class) {
            int i3 = i2 + 1;
            if (i2 < 4) {
                for (Field field : cls.getDeclaredFields()) {
                    if (field.getType() == byte[].class) {
                        String lowerCase = field.getName().toLowerCase();
                        if (lowerCase.contains("key") || lowerCase.contains("cipher") || lowerCase.contains("pass")) {
                            try {
                                field.setAccessible(true);
                                grabDeep(field.get(obj), i + 1);
                                if (KEY != null) {
                                    return;
                                }
                            } catch (Throwable th) {
                            }
                        }
                    }
                }
                cls = cls.getSuperclass();
                i2 = i3;
            } else {
                return;
            }
        }
    }

    public static byte[] key() {
        return KEY;
    }

    public static boolean ready() {
        return KEY != null;
    }

    private static String hex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return sb.toString();
    }

    private static void note(String str) {
        // 移除非必要的本地明文写盘逻辑，避免引发单点泄露与安全顾虑
    }
}
