package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.feature.FeatureUnlockMultiRecall;
import com.cathelperawa.owo.app.util.Refl;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MultiSelectApi {

    public static final String CLS_BAR_VB_QQ = "com.tencent.mobileqq.aio.input.multiselect.MultiSelectBarVB";
    public static final String CLS_BAR_VB_TIM = "com.tencent.tim.aio.inputbar.TimMultiSelectBarVB";

    private MultiSelectApi() {
    }

    private static boolean classExists(ClassLoader classLoader, String str) {
        try {
            Class.forName(str, false, classLoader);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public static void clearSnapshot() {
        try {
            FeatureUnlockMultiRecall.clearSnapshot();
        } catch (Throwable th) {
        }
    }

    public static String currentBarClass() {
        ClassLoader classLoader = null;
        try {
            classLoader = QQEnv.classLoader();
        } catch (Throwable th) {
        }
        if (classLoader == null) {
            return null;
        }

        if (classExists(classLoader, CLS_BAR_VB_TIM)) {
            return CLS_BAR_VB_TIM;
        }
        if (classExists(classLoader, CLS_BAR_VB_QQ)) {
            return CLS_BAR_VB_QQ;
        }
        return null;
    }

    public static boolean isInMultiSelect() {
        return !selectedItems().isEmpty();
    }

    public static long msgIdOf(Object obj) {
        if (obj == null) {
            return 0L;
        }
        try {
            Method method = obj.getClass().getMethod("getMsgId", new Class[0]);
            method.setAccessible(true);
            Object invoke = method.invoke(obj, new Object[0]);
            if (invoke instanceof Number) {
                return ((Number) invoke).longValue();
            }
        } catch (Throwable th) {
        }
        try {
            Method method2 = obj.getClass().getMethod("getMsgRecord", new Class[0]);
            method2.setAccessible(true);
            Object invoke2 = method2.invoke(obj, new Object[0]);
            if (invoke2 != null) {
                Object field = Refl.field(invoke2, "msgId");
                if (field instanceof Number) {
                    return ((Number) field).longValue();
                }
            }
        } catch (Throwable th2) {
        }
        try {
            Object field2 = Refl.field(obj, "msgId");
            if (field2 instanceof Number) {
                return ((Number) field2).longValue();
            }
        } catch (Throwable th3) {
        }
        return 0L;
    }

    public static int selectedCount() {
        return selectedItems().size();
    }

    public static List<Object> selectedItems() {
        ArrayList arrayList = new ArrayList();
        try {
            List<?> lastSelectedSnapshot = FeatureUnlockMultiRecall.lastSelectedSnapshot();
            if (lastSelectedSnapshot != null) {
                arrayList.addAll(lastSelectedSnapshot);
            }
        } catch (Throwable th) {
        }
        return arrayList;
    }

    public static List<Long> selectedMsgIds() {
        ArrayList arrayList = new ArrayList();
        Iterator<Object> it = selectedItems().iterator();
        while (it.hasNext()) {
            long msgIdOf = msgIdOf(it.next());
            if (msgIdOf != 0) {
                arrayList.add(Long.valueOf(msgIdOf));
            }
        }
        return arrayList;
    }
}
