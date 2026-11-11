package com.cathelperawa.owo.app.bridge;

import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.core.Host;
import com.cathelperawa.owo.app.proto.MediaVault;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class QqArkApi {
    private QqArkApi() {
    }

    

    

    public static void probe(ClassLoader classLoader) {
        char c;
        StringBuilder sb = new StringBuilder("── 卡片签名接口定位 ──\n");
        List<String> allClassNames = allClassNames(classLoader);
        char c2 = '\n';
        sb.append("dex 类总数 ").append(allClassNames.size()).append('\n');
        boolean z = false;
        int i = 0;
        for (String str : allClassNames) {
            String lowerCase = str.toLowerCase();
            if ((!lowerCase.contains(MediaVault.T_ARK) && !lowerCase.contains("share")) || lowerCase.startsWith("android.") || lowerCase.startsWith("java.")) {
                c2 = c2;
                z = false;
            } else {
                try {
                    Method[] declaredMethods = Class.forName(str, z, classLoader).getDeclaredMethods();
                    int length = declaredMethods.length;
                    for (int r9 = 0; r9 < length; r9++) {
                        Method method = declaredMethods[r9];
                        String name = method.getName();
                        if (name.contains("getArkMessageFromNet") || name.contains("ArkMessage")) {
                            StringBuilder sb2 = new StringBuilder();
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            int length2 = parameterTypes.length;
                            for (int r15 = 0; r15 < length2; r15++) {
                                try {
                                    sb2.append(parameterTypes[r15].getSimpleName()).append(',');
                                } catch (Throwable th) {
                                    c = '\n';
                                }
                            }
                            c = '\n';
                            try {
                                sb.append(str).append('#').append(name).append('(').append((CharSequence) sb2).append(") : ").append(method.getReturnType().getSimpleName()).append('\n');
                                i++;
                            } catch (Throwable th2) {
                            }
                        } else {
                            c = c2;
                        }
                        c2 = c;
                        z = false;
                    }
                    c = c2;
                } catch (Throwable th3) {
                    c = c2;
                }
                c2 = c;
                z = false;
            }
        }
        if (i == 0) {
            sb.append("（没找到，类名过滤条件可以再放宽）\n");
        }
        note(sb.toString());
        HookLog.i("QqArkApi: 命中 " + i);
    }

    private static List<String> allClassNames(ClassLoader classLoader) {
        Object obj = null;
        ArrayList arrayList = new ArrayList();
        try {
            Field declaredField = Class.forName("dalvik.system.BaseDexClassLoader", false, classLoader).getDeclaredField("pathList");
            declaredField.setAccessible(true);
            obj = declaredField.get(classLoader);
        } catch (Throwable th) {
            HookLog.w("QqArkApi 枚举失败 " + th);
        }
        if (obj == null) {
            return arrayList;
        }
        Object[] objArr;
        try {
            Field declaredField2 = obj.getClass().getDeclaredField("dexElements");
            declaredField2.setAccessible(true);
            objArr = (Object[]) declaredField2.get(obj);
        } catch (Throwable th) {
            return arrayList;
        }
        if (objArr == null) {
            return arrayList;
        }
        for (Object obj2 : objArr) {
            if (obj2 != null) {
                try {
                    Field declaredField3 = obj2.getClass().getDeclaredField("dexFile");
                    declaredField3.setAccessible(true);
                    Object obj3 = declaredField3.get(obj2);
                    if (obj3 != null) {
                        Method method = obj3.getClass().getMethod("entries", new Class[0]);
                        method.setAccessible(true);
                        Object invoke = method.invoke(obj3, new Object[0]);
                        if (invoke instanceof Enumeration) {
                            Enumeration enumeration = (Enumeration) invoke;
                            while (enumeration.hasMoreElements()) {
                                Object nextElement = enumeration.nextElement();
                                if (nextElement instanceof String) {
                                    arrayList.add((String) nextElement);
                                }
                            }
                        }
                    }
                } catch (Throwable th2) {
                }
            }
        }
        return arrayList;
    }

    private static void note(String str) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File("/data/user/0/" + Host.pkg() + "/files/arkapi.txt"), false);
            fileOutputStream.write(str.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th) {
        }
    }
}
