package com.cathelperawa.owo.app.bridge;

import android.database.Cursor;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Host;
import de.robv.android.xposed.XC_MethodHook;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QqDb {
    private static final Map<String, Object> OPEN = Collections.synchronizedMap(new HashMap());
    private static volatile boolean installed = false;

    private QqDb() {
    }

    public static synchronized void install(ClassLoader classLoader) {
        Class<?> findClass = null;
        synchronized (QqDb.class) {
            if (installed) {
                return;
            }
            try {
                findClass = HookApi.findClass(classLoader,
                        "com.tencent.mobileqq.app.SQLiteDatabase",   
                        "com.tencent.wcdb.database.SQLiteDatabase");
            } catch (Throwable th) {
                HookLog.w("QqDb 安装失败 " + th);
            }
            if (findClass == null) {
                HookLog.w("QqDb: 没有 SQLiteDatabase");
                return;
            }
            StringBuilder sb = new StringBuilder("== SQLiteDatabase methods ==\n");
            Method[] methods = findClass.getMethods();
            int length = methods.length;
            int i = 0;
            int i2 = 0;
            while (i < length) {
                Method method = methods[i];
                int i3 = i2 + 1;
                if (i2 > 120) {
                    break;
                }
                sb.append("  ").append(method.getName()).append(" : ").append(method.getReturnType().getSimpleName()).append('\n');
                i++;
                i2 = i3;
            }
            note(sb.toString());
            int i4 = 0;
            for (Method method2 : findClass.getMethods()) {
                if (method2.getReturnType().getName().endsWith("Cursor")) {
                    try {
                        method2.setAccessible(true);
                        HookApi.before(method2, new HookApi.Action() { 
                            @Override 
                            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                                try {
                                    QqDb.remember(methodHookParam.thisObject);
                                } catch (Throwable th2) {
                                }
                            }
                        });
                        i4++;
                    } catch (Throwable th2) {
                    }
                }
            }
            installed = i4 > 0;
            HookLog.i("QqDb: 挂了 " + i4 + " 个返回 Cursor 的方法");
            note("hooked(cursor)=" + i4);
        }
    }

    public static void remember(Object obj) {
        if (obj == null) {
            return;
        }
        try {
            Method method = obj.getClass().getMethod("getPath", new Class[0]);
            method.setAccessible(true);
            Object invoke = method.invoke(obj, new Object[0]);
            if (invoke == null) {
                return;
            }
            String lowerCase = String.valueOf(invoke).toLowerCase(Locale.US);
            Map<String, Object> map = OPEN;
            if (!map.containsKey(lowerCase)) {
                map.put(lowerCase, obj);
                note("cache " + lowerCase);
            }
        } catch (Throwable th) {
        }
    }

    public static Object db(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        Map<String, Object> map = OPEN;
        synchronized (map) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().contains(lowerCase)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    public static List<String> paths() {
        ArrayList arrayList;
        Map<String, Object> map = OPEN;
        synchronized (map) {
            arrayList = new ArrayList(map.keySet());
        }
        return arrayList;
    }

    public static List<String[]> query(String str, String str2, int i) {
        Method method = null;
        ArrayList arrayList = new ArrayList();
        Object db = db(str);
        if (db == null) {
            return arrayList;
        }
        Cursor cursor = null;
        try {
            try {
                Method[] methods = db.getClass().getMethods();
                int length = methods.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        method = null;
                        break;
                    }
                    method = methods[i2];
                    if ("rawQuery".equals(method.getName())) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 2 && parameterTypes[0] == String.class && parameterTypes[1] == String[].class) {
                            break;
                        }
                    }
                    i2++;
                }
            } catch (Throwable th) {
            }
        } catch (Throwable th2) {
            Throwable th = th2;
        }
        if (method == null) {
            return arrayList;
        }
        method.setAccessible(true);
        Object invoke;
        try {
            invoke = method.invoke(db, str2, null);
        } catch (Throwable th) {
            return arrayList;
        }
        if (!(invoke instanceof Cursor)) {
            return arrayList;
        }
        Cursor cursor2 = (Cursor) invoke;
        try {
            int columnCount = cursor2.getColumnCount();
            String[] strArr = new String[columnCount];
            for (int i3 = 0; i3 < columnCount; i3++) {
                strArr[i3] = cursor2.getColumnName(i3);
            }
            while (cursor2.moveToNext() && arrayList.size() < i) {
                String[] strArr2 = new String[columnCount];
                for (int i4 = 0; i4 < columnCount; i4++) {
                    try {
                        strArr2[i4] = cursor2.getString(i4);
                    } catch (Throwable th3) {
                        strArr2[i4] = "";
                    }
                }
                arrayList.add(strArr2);
            }
        } catch (Throwable th4) {
            cursor = cursor2;
            Throwable th = th4;
            try {
                HookLog.w("QqDb.query 失败 " + th);
                if (cursor != null) {
                    cursor.close();
                }
                return arrayList;
            } catch (Throwable th5) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th6) {
                    }
                }
                throw th5;
            }
        }
        if (cursor2 != null) {
            cursor2.close();
        }
        return arrayList;
    }

    public static void dumpTables() {
        StringBuilder sb = new StringBuilder("── 已缓存库 ──\n");
        Iterator<String> it = paths().iterator();
        while (it.hasNext()) {
            sb.append("  ").append(it.next()).append('\n');
        }
        for (String str : paths()) {
            sb.append("\n== ").append(str).append("\n");
            for (String[] strArr : query(str, "select name from sqlite_master where type='table' order by name", 80)) {
                if (strArr.length > 0) {
                    sb.append("   ").append(strArr[0]).append('\n');
                }
            }
        }
        note(sb.toString());
    }

    private static void note(String str) {
        try {
            File file = new File("/data/user/0/" + Host.pkg() + "/files/qqdb.txt");
            if (file.length() > 131072) {
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            try {
                fileOutputStream.write(str.getBytes("UTF-8"));
                fileOutputStream.close();
            } catch (Throwable th) {
                fileOutputStream.close();
                throw th;
            }
        } catch (Throwable th2) {
        }
    }
}
