package com.cathelperawa.owo.app.feature;

import com.cathelperawa.owo.app.core.LoadPriority;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.DependsOn;
import com.cathelperawa.owo.app.core.SelfCheckResult;
import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.cathelperawa.owo.app.MsgStore;
import com.cathelperawa.owo.app.api.MsgApi;
import com.cathelperawa.owo.app.api.MultiSelectApi;
import com.cathelperawa.owo.app.bridge.ContactFactory;
import com.cathelperawa.owo.app.bridge.NtMsgApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.QQUtils;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookConfig;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.ui.UiKit;
import com.cathelperawa.owo.app.util.Refl;
import de.robv.android.xposed.XC_MethodHook;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.cathelperawa.owo.app.core.dex.ClassResolver;

@DependsOn({"core.dex"})
@HookItem(description = "长按进多选，批量撤回自己的消息", name = "多选撤回", order = 62, path = "去除限制/多选撤回",
        priority = LoadPriority.DEFERRED)

public class FeatureUnlockMultiRecall extends HookItemBase {

    @Override
    protected void check(SelfCheckResult.Checker c) {
        c.cls("AIO 上下文", CLS_AIO_CTX);
        c.cls("多选工具", CLS_MULTI_UTIL);
        c.cls("消息项", CLS_MSG_ITEM);
        c.cls("多选栏 VB", CLS_BAR_VB);
        c.cls("气泡 VB", CLS_BUBBLE_VB);
        c.cls("多选事件", "com.tencent.mobileqq.aio.event.MultiSelectEvent$SetSelectMode");
    }

    private Class<?> cls(String name) {
        return ClassResolver.find(this.cl, "multi:" + name, name);
    }

    private static final String CLS_AIO_CTX = "com.tencent.aio.api.f.a";
    private static final String CLS_BAR_VB = "com.tencent.tim.aio.inputbar.TimMultiSelectBarVB";
    private static final String CLS_BUBBLE_VB = "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";
    private static final String CLS_FORWARD_HELPER = "com.tencent.mobileqq.aio.helper.MultiForwardHelper";
    private static final String CLS_MSG_ITEM = "com.tencent.mobileqq.aio.msg.AIOMsgItem";
    private static final String CLS_MULTI_UTIL = "com.tencent.mobileqq.aio.msglist.holder.component.multifoward.b";
    private static final String CLS_VM_BASE = "com.tencent.c.a.b.b";
    private static final int ID_RECALL_BTN = 2131689676;
    public static final String K = "unlock_multirevoke";

    private static final long RECALL_GAP_MS = 60;

    private static final long RECALL_TAIL_MS = 260;
    private static volatile Object sAioCtx;
    private static volatile Object sBarVB;
    private static volatile List<?> sLastSelected;
    private static final LinkedHashMap<Long, Object> SELECTED_MSGS = new LinkedHashMap<>();
    private static final LinkedHashMap<Long, Object> MSG_ITEM_POOL = new LinkedHashMap<>();
    private static volatile boolean sRecalling = false;

    public static final class RecallArrowDrawable extends Drawable {
        private static final float SRC = 72.0f;
        private static final float STROKE = 0.083333336f;
        private static final float X_BEND = 0.5833333f;
        private static final float X_LEFT = 0.13194445f;
        private static final float X_TIP = 0.33333334f;
        private static final float Y_BOT = 0.7847222f;
        private static final float Y_MID = 0.2847222f;
        private static final float Y_TIP_DN = 0.4861111f;
        private static final float Y_TIP_UP = 0.083333336f;
        private final Paint p;
        private final Path path;
        private final int size;

        RecallArrowDrawable(int i, int i2) {
            Paint paint = new Paint(1);
            this.p = paint;
            this.path = new Path();
            paint.setColor(i);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            this.size = i2;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float width = bounds.width();
            float height = bounds.height();
            float f = bounds.left;
            float f2 = bounds.top;
            this.p.setStrokeWidth(width * 0.083333336f);
            float f3 = (X_LEFT * width) + f;
            float f4 = f + (X_BEND * width);
            float f5 = (Y_MID * height) + f2;
            float f6 = f2 + (Y_BOT * height);
            this.path.reset();
            this.path.moveTo(f3, f5);
            this.path.lineTo(f4, f5);
            float f7 = (f6 - f5) / 2.0f;
            float f8 = 0.5523f * f7;
            float f9 = f4 + f8;
            float f10 = f4 + f7;
            float f11 = f7 + f5;
            this.path.cubicTo(f9, f5, f10, f11 - f8, f10, f11);
            this.path.cubicTo(f10, f11 + f8, f9, f6, f4, f6);
            this.path.lineTo(f3, f6);
            canvas.drawPath(this.path, this.p);
            this.path.reset();
            Path path = this.path;
            float f12 = f + (width * X_TIP);
            path.moveTo(f12, (0.083333336f * height) + f2);
            this.path.lineTo(f3, f5);
            this.path.lineTo(f12, f2 + (height * Y_TIP_DN));
            canvas.drawPath(this.path, this.p);
        }

        @Override
        public int getIntrinsicHeight() {
            return this.size;
        }

        @Override
        public int getIntrinsicWidth() {
            return this.size;
        }

        @Override
        public int getOpacity() {
            return -3;
        }

        @Override
        public void setAlpha(int i) {
            this.p.setAlpha(i);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.p.setColorFilter(colorFilter);
        }
    }

    public void addRecallButton(LinearLayout linearLayout, final Object obj) {
        Context context = linearLayout.getContext();
        float f = context.getResources().getDisplayMetrics().density;
        int i = (int) (24.0f * f);
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(new RecallArrowDrawable(-11711155, i));
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setId(ID_RECALL_BTN);
        int i2 = (int) (40.0f * f);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(i, i);
        layoutParams.gravity = 17;
        frameLayout.addView(imageView, layoutParams);
        int i3 = (int) (f * 4.0f);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(i2, i2);
        layoutParams2.gravity = 16;
        layoutParams2.leftMargin = i3;
        layoutParams2.rightMargin = i3;
        frameLayout.setLayoutParams(layoutParams2);
        frameLayout.setBackgroundDrawable(rippleBg());
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FeatureUnlockMultiRecall.this.exitMultiSelect();
                FeatureUnlockMultiRecall.this.doBatchRecall(obj);
            }
        });
        writeNow("diag_multi.txt", "[开始] clear");
        int max = Math.max(0, linearLayout.getChildCount() - 1);
        linearLayout.addView(frameLayout, max);
        writeNow("diag_multi.txt", "[插入] 宿主=" + linearLayout.getClass().getName() + " 子数=" + linearLayout.getChildCount() + " 位置=" + max + " VB=" + (obj == null ? "null" : obj.getClass().getName()));
    }

    private Object callNoArgReturning(Object obj, Class<?> cls) {
        if (obj == null) {
            return null;
        }
        Class<?> cls2 = obj.getClass();
        for (int i = 0; cls2 != null && i < 8; i++) {
            for (Method method : cls2.getDeclaredMethods()) {
                if (method.getParameterTypes().length == 0 && cls.isAssignableFrom(method.getReturnType()) && method.getDeclaringClass() != Object.class) {
                    try {
                        method.setAccessible(true);
                        Object invoke = method.invoke(obj, new Object[0]);
                        if (invoke != null) {
                            writeNow("diag_multi.txt", "[退路·无参方法命中] " + cls2.getSimpleName() + "." + method.getName() + "() -> " + invoke.getClass().getSimpleName());
                            return invoke;
                        }
                        continue;
                    } catch (Throwable th) {
                    }
                }
            }
            cls2 = cls2.getSuperclass();
        }
        return null;
    }

    private boolean clearOnRemoveAll(ClassLoader classLoader, String str) {
        try {
            for (Constructor<?> constructor : Class.forName(str, false, classLoader).getDeclaredConstructors()) {
                hookAfter(constructor, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                        synchronized (FeatureUnlockMultiRecall.SELECTED_MSGS) {
                            FeatureUnlockMultiRecall.SELECTED_MSGS.clear();
                        }
                        FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[③ 清空全部]");
                    }
                });
            }
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public static void clearSnapshot() {
        sLastSelected = null;
    }

    private void collectMsgItems(Object obj, List<Object> list, int i) {
        if (obj == null || i > 20 || list.size() > 200) {
            return;
        }
        try {
            Class<?> cls = obj.getClass();
            for (int i2 = 0; cls != null && i2 < 4; i2++) {
                for (Field field : cls.getDeclaredFields()) {
                    if (field.getType().getName().equals(CLS_MSG_ITEM)) {
                        try {
                            field.setAccessible(true);
                            Object obj2 = field.get(obj);
                            if (obj2 != null && !containsSame(list, obj2)) {
                                list.add(obj2);
                            }
                        } catch (Throwable th) {
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
            if (obj instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) obj;
                for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                    collectMsgItems(viewGroup.getChildAt(i3), list, i + 1);
                }
            }
        } catch (Throwable th2) {
        }
    }

    private boolean containsSame(List<Object> list, Object obj) {
        Iterator<Object> it = list.iterator();
        while (it.hasNext()) {
            if (it.next() == obj) {
                return true;
            }
        }
        return false;
    }

    private Object currentContact() {
        Object next;
        Object recordOf;
        try {
            LinkedHashMap<Long, Object> linkedHashMap = MSG_ITEM_POOL;
            synchronized (linkedHashMap) {
                Iterator<Object> it = linkedHashMap.values().iterator();
                next = it.hasNext() ? it.next() : null;
            }
            if (next == null) {
                Activity activity = QQEnv.topActivity();
                if (activity instanceof Activity) {
                    ArrayList arrayList = new ArrayList();
                    Activity activity2 = activity;
                    collectMsgItems(activity.findViewById(R.id.content), arrayList, 0);
                    if (!arrayList.isEmpty()) {
                        next = arrayList.get(0);
                    }
                }
            }
            if (next == null || (recordOf = getRecordOf(next)) == null) {
                return null;
            }
            int i32 = Refl.i32(Refl.field(recordOf, "chatType"), -1);
            String str = Refl.str(Refl.field(recordOf, "peerUid"));
            String str2 = Refl.str(Refl.field(recordOf, "guildId"));
            writeNow("diag_multi.txt", "[Contact] chatType=" + i32 + " peerUid=" + str + " guild=" + str2);
            if (str2 == null) {
                str2 = "";
            }
            return ContactFactory.make(i32, str, str2);
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[Contact异常] " + th);
            return null;
        }
    }

    private void diag(ClassLoader classLoader) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        try {
            String[] strArr = {"com.tencent.tim.aio.inputbar.TimMultiSelectBarVB", MultiSelectApi.CLS_BAR_VB_QQ, CLS_MULTI_UTIL, CLS_VM_BASE};
            for (int i = 0; i < 4; i++) {
                String str = strArr[i];
                Class<?> findClass = HookApi.findClass(classLoader, str);
                sb.append(shortName(str)).append('=').append(findClass == null ? "无" : "有").append('\n');
                if (findClass != null) {
                    for (Method method : findClass.getDeclaredMethods()) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Class<?> returnType = method.getReturnType();
                        if (Modifier.isPublic(method.getModifiers())) {
                            if (!View.class.isAssignableFrom(returnType) && !List.class.isAssignableFrom(returnType) && parameterTypes.length > 1) {
                                z = false;
                                if (!z) {
                                    sb.append("    ").append(method.getName()).append('(');
                                    int i2 = 0;
                                    while (i2 < parameterTypes.length) {
                                        sb.append(i2 > 0 ? "," : "").append(parameterTypes[i2].getSimpleName());
                                        i2++;
                                    }
                                    sb.append(") : ").append(returnType.getSimpleName()).append('\n');
                                }
                            }
                            z = true;
                            if (!z) {
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            sb.append("诊断异常 ").append(th).append('\n');
        }
        writeNow("diag_multi.txt", sb.toString());
        HookLog.i("多选撤回：诊断已写");
    }

    public void doBatchRecall(Object obj) {
        if (obj != null) {
            sBarVB = obj;
        }
        final Object obj2 = sBarVB;
        if (sRecalling) {
            writeNow("diag_multi.txt", "[点按钮] 已在撤回中，忽略");
        } else {
            sRecalling = true;
            Bg.run(new Runnable() {
                @Override
                public void run() {
                    List selectedMsgItemsByProbe;
                    Class<?> cls;
                    try {
                        ArrayList arrayList = new ArrayList();
                        List list = FeatureUnlockMultiRecall.sLastSelected;
                        if (list == null || list.isEmpty()) {
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[点按钮] 原生列表为空（没选中消息）");
                        } else {
                            arrayList.addAll(list);
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[点按钮] ★用 TIM 原生选中列表 " + list.size() + " 条");
                        }
                        if (arrayList.isEmpty()) {
                            FeatureUnlockMultiRecall.this.toastOnUi("还没选消息呢喵…先点几条嘛~");
                            return;
                        }
                        try {
                            Object vmFromVb = FeatureUnlockMultiRecall.this.getVmFromVb(obj2, null);
                            Object ctxFromVm = (vmFromVb == null || (cls = Class.forName(FeatureUnlockMultiRecall.CLS_AIO_CTX, false, FeatureUnlockMultiRecall.this.cl)) == null) ? null : FeatureUnlockMultiRecall.this.getCtxFromVm(vmFromVb, cls);
                            if (ctxFromVm == null) {
                                ctxFromVm = FeatureUnlockMultiRecall.this.grabAioCtxNow(obj2);
                            }
                            if (ctxFromVm == null) {
                                ctxFromVm = FeatureUnlockMultiRecall.sAioCtx;
                            }
                            if (ctxFromVm != null && (selectedMsgItemsByProbe = FeatureUnlockMultiRecall.this.getSelectedMsgItemsByProbe(ctxFromVm)) != null && !selectedMsgItemsByProbe.isEmpty()) {
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[点按钮] 探路结果 " + selectedMsgItemsByProbe.size() + " 条");
                                arrayList.clear();
                                arrayList.addAll(selectedMsgItemsByProbe);
                            }
                        } catch (Throwable th) {
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[探路异常] " + th);
                        }
                        if (arrayList.isEmpty()) {
                            FeatureUnlockMultiRecall.this.toastOnUi("还没选消息呢喵…先点几条嘛~");
                            return;
                        }
                        int size = arrayList.size();
                        int i = 0;
                        int i2 = 0;
                        MsgStore.add("[多选撤回] 开始撤回 " + size + " 条");

                        ArrayList<Long> ids = new ArrayList<>();
                        Object contact = null;
                        long tStart = System.currentTimeMillis();
                        for (Object it : arrayList) {
                            try {
                                Object rec = FeatureUnlockMultiRecall.this.recordOf(it);
                                if (rec == null) { i2++; continue; }
                                long mid = FeatureUnlockMultiRecall.this.msgIdOf2(rec);
                                if (mid == 0) { i2++; continue; }
                                if (contact == null) {
                                    int ct = FeatureUnlockMultiRecall.this.chatTypeOf(rec);
                                    String pu = FeatureUnlockMultiRecall.this.peerUidOf(rec);
                                    String gid = FeatureUnlockMultiRecall.this.guildIdOf(rec);
                                    contact = ContactFactory.make(ct, pu, gid);
                                    FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt",
                                            "[连发] contact chatType=" + ct + " peer=" + pu + " 条数=" + size);
                                    if (contact == null) { i2++; continue; }
                                }
                                ids.add(Long.valueOf(mid));
                            } catch (Throwable th2) {
                                i2++;
                                HookLog.e("多选撤回：取 msgId 失败", th2);
                            }
                        }
                        int rc = Integer.MIN_VALUE;
                        if (contact != null && !ids.isEmpty()) {
                            rc = FeatureUnlockMultiRecall.this.fireRecallBurst(contact, ids);
                            if (rc == 0) i = ids.size();
                            else i2 += ids.size();
                            try { Thread.sleep(FeatureUnlockMultiRecall.RECALL_TAIL_MS); } catch (Throwable ignore) {}
                        }
                        FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt",
                                "[连发] 提交 " + ids.size() + " 条 耗时 " + (System.currentTimeMillis() - tStart) + "ms");
                        MsgStore.add("[多选撤回] 完成：成功 " + i + " / 失败 " + i2 + " / 共 " + size);
                        if (i2 == 0) {
                            FeatureUnlockMultiRecall.this.toastOnUi("撤回啦～" + i + " 条都收回来了喵！");
                        } else if (i == 0) {

                            String why = rc == -1 ? "调用没成功" : ("服务端拒绝 code=" + rc);
                            FeatureUnlockMultiRecall.this.toastOnUi(
                                    "一条都没撤回来喵…（" + why + "）");
                        } else {
                            FeatureUnlockMultiRecall.this.toastOnUi("撤回来 " + i + " 条，还有 " + i2 + " 条太久了撤不动喵…");
                        }
                        synchronized (FeatureUnlockMultiRecall.SELECTED_MSGS) {
                            FeatureUnlockMultiRecall.SELECTED_MSGS.clear();
                        }
                        List unused = FeatureUnlockMultiRecall.sLastSelected = null;
                    } finally {
                        boolean unused2 = FeatureUnlockMultiRecall.sRecalling = false;
                    }
                }
            }, "cat-multi-recall");
        }
    }

    private void exitMultiSelect() {
        try {
            final Activity activity = QQEnv.topActivity();
            if (activity instanceof Activity) {
                Activity activity2 = activity;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (FeatureUnlockMultiRecall.this.leaveMultiSelectOnly()) return;
                        try {
                            ((Activity) activity).onBackPressed();
                        } catch (Throwable th) {
                        }
                    }
                });
            }
        } catch (Throwable th) {
        }
    }

    private boolean leaveMultiSelectOnly() {
        Object ctx = null;
        try {
            Object vb = sBarVB;
            if (vb != null) ctx = getVmFromVb(vb, null);
        } catch (Throwable ignore) {
        }
        if (ctx == null) ctx = sAioCtx;
        if (ctx == null) {
            writeNow("diag_multi.txt", "[退出多选] 没拿到 AIO 上下文，回退 onBackPressed");
            return false;
        }
        try {
            Method route = null;
            for (Method m : ctx.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && "e".equals(m.getName())
                        && "com.tencent.mvi.base.route.j".equals(m.getReturnType().getName())) {
                    route = m;
                    break;
                }
            }
            if (route == null) {
                writeNow("diag_multi.txt", "[退出多选] 没找到 e() 路由");
                return false;
            }
            route.setAccessible(true);
            Object router = route.invoke(ctx);
            if (router == null) return false;
            Method send = null;
            for (Method m : router.getClass().getMethods()) {
                if ("h".equals(m.getName()) && m.getParameterTypes().length == 1
                        && "com.tencent.mvi.base.route.MsgIntent".equals(m.getParameterTypes()[0].getName())) {
                    send = m;
                    break;
                }
            }
            if (send == null) {
                writeNow("diag_multi.txt", "[退出多选] 没找到 send(h)");
                return false;
            }
            send.setAccessible(true);
            int sent = 0;

            try {
                Class<?> a = ClassResolver.find(cl, "aio.event.setSelectMode",
                        "com.tencent.mobileqq.aio.event.MultiSelectEvent$SetSelectMode");
                Object ev = a.getConstructor(boolean.class).newInstance(Boolean.FALSE);
                send.invoke(router, ev);
                sent++;
            } catch (Throwable t) {
                writeNow("diag_multi.txt", "[退出多选] SetSelectMode 失败 " + t);
            }

            try {
                Class<?> b = ClassResolver.find(cl, "aio.event.msModeChange",
                        "com.tencent.mobileqq.aio.event.AIOMsgListEvent$MultiSelectModeChangeEvent");
                Object ev = b.getConstructor(String.class, boolean.class).newInstance("MultiSelectBarVM", Boolean.FALSE);
                send.invoke(router, ev);
                sent++;
            } catch (Throwable t2) {
                writeNow("diag_multi.txt", "[退出多选] ModeChangeEvent 失败 " + t2);
            }

            try {
                Class<?> c = ClassResolver.find(cl, "aio.event.exitMultiForward",
                        "com.tencent.mobileqq.aio.event.MsgForwardEvent$ExitMultiForward");
                Object ev = null;
                try {
                    Field f = c.getDeclaredField("d");
                    f.setAccessible(true);
                    ev = f.get(null);
                } catch (Throwable ignore) {
                }
                if (ev == null) {
                    Constructor<?> ctor = c.getDeclaredConstructor(new Class[0]);
                    ctor.setAccessible(true);
                    ev = ctor.newInstance(new Object[0]);
                }
                send.invoke(router, ev);
                sent++;
            } catch (Throwable t3) {
                writeNow("diag_multi.txt", "[退出多选] ExitMultiForward 失败 " + t3);
            }
            writeNow("diag_multi.txt", "[退出多选] 已发 " + sent + "/3 条事件");
            return sent > 0;
        } catch (Throwable t) {
            writeNow("diag_multi.txt", "[退出多选异常] " + t);
            return false;
        }
    }

    public Object getCtxFromVm(Object obj, Class<?> cls) {
        if (obj == null) {
            return null;
        }
        char c = '.';
        try {
            Class<?> cls2 = obj.getClass();
            Method method = null;
            for (int i = 0; cls2 != null && method == null && i < 8; i++) {
                Method[] declaredMethods = cls2.getDeclaredMethods();
                int length = declaredMethods.length;
                int i2 = 0;
                while (i2 < length) {
                    Method method2 = declaredMethods[i2];
                    if (method2.getName().equals("H") && method2.getParameterTypes().length == 0) {
                        method = method2;
                        break;
                    }
                    i2++;
                }
                cls2 = cls2.getSuperclass();
            }
            writeNow("diag_multi.txt", "[VM→上下文] 没找到 H()");
        } catch (Throwable th) {
            if (th instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) th;
                if (invocationTargetException.getCause() != null) {
                    th = invocationTargetException.getCause();
                }
            }
            StackTraceElement[] stackTrace = th.getStackTrace();
            StringBuilder sb = new StringBuilder("[VM.H异常] " + th + " | ");
            for (int i3 = 0; i3 < Math.min(stackTrace.length, 5); i3++) {
                sb.append(stackTrace[i3].getClassName()).append('.').append(stackTrace[i3].getMethodName()).append(':').append(stackTrace[i3].getLineNumber()).append(" | ");
            }
            writeNow("diag_multi.txt", sb.toString());
        }
        try {
            StringBuilder sb2 = new StringBuilder("[VM无参方法] ");
            Class<?> cls3 = obj.getClass();
            int i4 = 0;
            while (cls3 != null && i4 < 6) {
                Method[] declaredMethods2 = cls3.getDeclaredMethods();
                int length2 = declaredMethods2.length;
                int i5 = 0;
                while (i5 < length2) {
                    Method method3 = declaredMethods2[i5];
                    if (method3.getParameterTypes().length == 0 && method3.getReturnType() != Void.TYPE) {
                        String name = method3.getReturnType().getName();
                        if (!name.startsWith("java.") && !name.startsWith("kotlin.")) {
                            try {
                                method3.setAccessible(true);
                                Object invoke = method3.invoke(obj, new Object[0]);
                                sb2.append(cls3.getSimpleName()).append(c).append(method3.getName()).append("()=").append(invoke == null ? "null" : invoke.getClass().getName()).append("  ");
                                if (invoke != null && cls.isInstance(invoke)) {
                                    writeNow("diag_multi.txt", "[VM→上下文] " + method3.getName() + "() = " + invoke.getClass().getSimpleName());
                                    return invoke;
                                }
                            } catch (Throwable th2) {
                            }
                        }
                    }
                    i5++;
                    c = '.';
                }
                cls3 = cls3.getSuperclass();
                i4++;
                c = '.';
            }
            writeNow("diag_multi.txt", sb2.toString());
            return null;
        } catch (Throwable th3) {
            writeNow("diag_multi.txt", "[VM扫描异常] " + th3);
            return null;
        }
    }

    private Object getRecordOf(Object obj) {
        try {
            Method method = obj.getClass().getMethod("getMsgRecord", new Class[0]);
            method.setAccessible(true);
            return method.invoke(obj, new Object[0]);
        } catch (Throwable th) {
            return null;
        }
    }

    private List<?> getSelectedMsgItems(Object obj) throws Throwable {
        Object obj2;
        Method method;
        String str;
        List<?> readVmSelectedList;
        Object obj3;
        int i;
        String str2;
        String str3 = "null";
        try {
            int i2 = 0;
            Class<?> cls = cls(CLS_MULTI_UTIL);
            try {
                Field declaredField = cls.getDeclaredField("a");
                declaredField.setAccessible(true);
                obj2 = declaredField.get(null);
            } catch (Throwable th) {
                obj2 = null;
            }
            if (obj2 == null) {
                Method[] declaredMethods = cls.getDeclaredMethods();
                int length = declaredMethods.length;
                Object obj4 = obj2;
                int i3 = 0;
                while (true) {
                    if (i3 >= length) {
                        obj2 = obj4;
                        break;
                    }
                    Method method2 = declaredMethods[i3];
                    if (Modifier.isStatic(method2.getModifiers()) && method2.getParameterTypes().length == 0 && method2.getReturnType() == cls) {
                        try {
                            method2.setAccessible(true);
                            obj2 = method2.invoke(null, new Object[0]);
                        } catch (Throwable th2) {
                            obj2 = obj4;
                        }
                        if (obj2 != null) {
                            break;
                        }
                        obj4 = obj2;
                    }
                    i3++;
                }
            }
            if (obj2 == null) {
                HookLog.w("多选撤回：拿不到 multifoward.b 单例");
                return null;
            }
            Method[] declaredMethods2 = cls.getDeclaredMethods();
            int length2 = declaredMethods2.length;
            int i4 = 0;
            while (true) {
                if (i4 >= length2) {
                    method = null;
                    break;
                }
                method = declaredMethods2[i4];
                if (method.getParameterTypes().length == 1 && List.class.isAssignableFrom(method.getReturnType()) && method.getParameterTypes()[0].isInstance(obj)) {
                    break;
                }
                i4++;
            }
            if (method == null) {
                HookLog.w("多选撤回：未找到取列表方法");
                return null;
            }
            method.setAccessible(true);
            Object invoke = method.invoke(obj2, obj);
            boolean z = invoke instanceof List;
            int size = z ? ((List) invoke).size() : -1;
            HookLog.i("多选撤回：取列表方法 = " + method.getName() + "，结果 = " + size + " 条");
            writeNow("diag_multi.txt", "[取列表] 方法=" + method.getName() + " 参数=" + method.getParameterTypes()[0].getSimpleName() + " 结果=" + size + " 条 aioCtx=" + obj.getClass().getName());
            if (z) {
                List<?> list = (List) invoke;
                if (!list.isEmpty()) {
                    return list;
                }
            }
            try {
                Class<?> cls2 = ClassResolver.find(this.cl, "aio.event.getSelectMsgList",
                    "com.tencent.mobileqq.aio.event.MultiSelectEvent$GetSelectMsgList");
                if (cls2 != null) {
                    Object newInstance = cls2.newInstance();
                    try {
                        Class<?> cls3 = obj.getClass();
                        Method method3 = null;
                        while (cls3 != null && method3 == null) {
                            Method[] declaredMethods3 = cls3.getDeclaredMethods();
                            int length3 = declaredMethods3.length;
                            int i5 = i2;
                            while (i5 < length3) {
                                Method method4 = declaredMethods3[i5];
                                if (method4.getParameterTypes().length == 0 && "com.tencent.mvi.base.route.j".equals(method4.getReturnType().getName())) {
                                    method3 = method4;
                                    break;
                                }
                                i5++;
                            }
                            cls3 = cls3.getSuperclass();
                            i2 = 0;
                        }
                        if (method3 != null) {
                            method3.setAccessible(true);
                            obj3 = method3.invoke(obj, new Object[0]);
                        } else {
                            obj3 = null;
                        }
                    } catch (Throwable th3) {
                        obj3 = null;
                    }
                    writeNow("diag_multi.txt", "[退路A] route=" + (obj3 == null ? "null" : obj3.getClass().getName()));
                    if (obj3 != null) {
                        Class<?> cls4 = obj3.getClass();
                        Method method5 = null;
                        while (cls4 != null && method5 == null) {
                            Method[] declaredMethods4 = cls4.getDeclaredMethods();
                            int length4 = declaredMethods4.length;
                            int i6 = 0;
                            while (true) {
                                if (i6 >= length4) {
                                    str = str3;
                                    break;
                                }
                                Method method6 = declaredMethods4[i6];
                                str = str3;
                                if (method6.getParameterTypes().length == 1) {
                                    try {
                                        if (method6.getParameterTypes()[0].isAssignableFrom(cls2)) {
                                            method5 = method6;
                                            break;
                                        }
                                    } catch (Throwable th4) {
                                        writeNow("diag_multi.txt", "[退路A异常] " + th4);
                                        readVmSelectedList = readVmSelectedList();
                                        writeNow("diag_multi.txt", "[取列表·退路VM] = " + (readVmSelectedList != null ? str : readVmSelectedList.size() + " 条"));
                                        if (readVmSelectedList == null) {
                                        }
                                        if (z) {
                                        }
                                    }
                                }
                                i6++;
                                str3 = str;
                            }
                            cls4 = cls4.getSuperclass();
                            str3 = str;
                        }
                        str = str3;
                        StringBuilder append = new StringBuilder().append("[退路A] h方法=");
                        if (method5 == null) {
                            str2 = "没找到";
                            i = 0;
                        } else {
                            i = 0;
                            str2 = method5.getName() + "(" + method5.getParameterTypes()[0].getSimpleName() + ")";
                        }
                        writeNow("diag_multi.txt", append.append(str2).toString());
                        if (method5 != null) {
                            method5.setAccessible(true);
                            method5.invoke(obj3, newInstance);
                            while (cls2 != null) {
                                Field[] declaredFields = cls2.getDeclaredFields();
                                int length5 = declaredFields.length;
                                for (int i7 = i; i7 < length5; i7++) {
                                    Field field = declaredFields[i7];
                                    if (List.class.isAssignableFrom(field.getType())) {
                                        field.setAccessible(true);
                                        Object obj5 = field.get(newInstance);
                                        if (obj5 instanceof List) {
                                            writeNow("diag_multi.txt", "[退路A] 事件字段 " + cls2.getSimpleName() + "." + field.getName() + " = " + ((List) obj5).size() + " 条");
                                            if (!((List) obj5).isEmpty()) {
                                                return (List) obj5;
                                            }
                                        } else {
                                            continue;
                                        }
                                    }
                                }
                                cls2 = cls2.getSuperclass();
                            }
                        }
                    } else {
                        str = "null";
                    }
                } else {
                    str = "null";
                }
            } catch (Throwable th5) {
                str = str3;
            }
            readVmSelectedList = readVmSelectedList();
            writeNow("diag_multi.txt", "[取列表·退路VM] = " + (readVmSelectedList != null ? str : readVmSelectedList.size() + " 条"));
            if (readVmSelectedList == null && !readVmSelectedList.isEmpty()) {
                return readVmSelectedList;
            }
            if (z) {
                return null;
            }
            return (List) invoke;
        } catch (Throwable th6) {
            writeNow("diag_multi.txt", "[取列表] 类加载失败 " + th6);
            return null;
        }
    }

    public List<?> getSelectedMsgItemsByProbe(Object obj) {
        Object obj2;
        ArrayList arrayList = new ArrayList();
        try {
            char c = 0;
            Class<?> cls = cls(CLS_MULTI_UTIL);
            Class<?> cls2 = cls(CLS_MSG_ITEM);
            try {
                Field declaredField = cls.getDeclaredField("a");
                declaredField.setAccessible(true);
                obj2 = declaredField.get(null);
            } catch (Throwable th) {
                obj2 = null;
            }
            if (obj2 == null) {
                writeNow("diag_multi.txt", "[探路] 单例没拿到");
                return null;
            }
            Method[] declaredMethods = cls.getDeclaredMethods();
            int length = declaredMethods.length;
            int i = -1;
            int i2 = 0;
            while (i2 < length) {
                Method method = declaredMethods[i2];
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[c].isInstance(obj) && method.getReturnType() == Integer.TYPE) {
                    try {
                        method.setAccessible(true);
                        Object invoke = method.invoke(obj2, obj);
                        if (invoke instanceof Number) {
                            writeNow("diag_multi.txt", "[探路] " + method.getName() + "(ctx) = " + invoke);
                            if (((Number) invoke).intValue() > 0) {
                                i = ((Number) invoke).intValue();
                            }
                        }
                    } catch (Throwable th2) {
                    }
                }
                i2++;
                c = 0;
            }
            if (i <= 0) {
                writeNow("diag_multi.txt", "[探路] f/g 总数为 0（单例可能不对），改为只靠逐条 IsMsgSelect 判断");
            }
            List<?> pullRecentMsgs = pullRecentMsgs(obj);
            writeNow("diag_multi.txt", "[探路] 候选消息 = " + (pullRecentMsgs == null ? "null" : pullRecentMsgs.size() + " 条"));
            if (pullRecentMsgs == null) {
                return null;
            }
            for (Object obj3 : pullRecentMsgs) {
                if (!cls2.isInstance(obj3)) {
                    obj3 = toMsgItem(obj3);
                }
                if (obj3 != null) {
                    if (isItemSelected(obj2, obj, obj3)) {
                        arrayList.add(obj3);
                        writeNow("diag_multi.txt", "[探路·命中选中] msgId=" + msgIdOf(obj3));
                    }
                }
            }
            writeNow("diag_multi.txt", "[探路] 判定选中 = " + arrayList.size() + " 条 (sum=" + i + ")");
            if (arrayList.isEmpty()) {
                return null;
            }
            return arrayList;
        } catch (Throwable th3) {
            writeNow("diag_multi.txt", "[探路异常] " + th3);
            if (arrayList.isEmpty()) {
                return null;
            }
            return arrayList;
        }
    }

    public Object getVmFromVb(Object obj, Class<?> cls) {
        Class<?> returnType;
        Method findMethodByType;
        if (obj == null) {
            return null;
        }
        try {
            Class<?> cls2 = ClassResolver.find(this.cl, "mvi.baseVM", "com.tencent.mvi.mvvm.BaseVM");
            if (cls2 != null && (findMethodByType = Refl.findMethodByType(obj.getClass(), cls2, new Class[0])) != null) {
                findMethodByType.setAccessible(true);
                Object invoke = findMethodByType.invoke(obj, new Object[0]);
                if (invoke != null) {
                    return invoke;
                }
            }
        } catch (Throwable th) {
        }
        for (Method method : obj.getClass().getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0 && (returnType = method.getReturnType()) != Void.TYPE && !View.class.isAssignableFrom(returnType) && !Context.class.isAssignableFrom(returnType) && !returnType.getName().startsWith("java.")) {
                try {
                    method.setAccessible(true);
                    Object invoke2 = method.invoke(obj, new Object[0]);
                    if (invoke2 != null && isVmLike(invoke2)) {
                        return invoke2;
                    }
                } catch (Throwable th2) {
                }
            }
        }
        return null;
    }

    public Object grabAioCtxNow(Object obj) {
        Class<?> cls = null;
        Object scanForCtx;
        try {
            cls = cls(CLS_AIO_CTX);
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[退路异常] " + th);
        }
        if (cls == null) {
            writeNow("diag_multi.txt", "[退路] AIO上下文类加载失败");
            return null;
        }
        if (obj != null) {
            Object scanForCtx2 = scanForCtx(obj, cls, 0);
            if (scanForCtx2 != null) {
                writeNow("diag_multi.txt", "[退路·VB字段命中] " + scanForCtx2.getClass().getName());
                return scanForCtx2;
            }
            writeNow("diag_multi.txt", "[退路] VB 字段里没扫到上下文");
        }
        Activity activity = QQEnv.topActivity();
        if (activity != null) {
            if (cls.isInstance(activity)) {
                return activity;
            }
            Object scanForCtx3 = scanForCtx(activity, cls, 0);
            if (scanForCtx3 != null) {
                writeNow("diag_multi.txt", "[退路·Activity字段命中] " + scanForCtx3.getClass().getName());
                return scanForCtx3;
            }
        }
        Object vmFromVb = getVmFromVb(obj, cls);
        if (vmFromVb != null && (scanForCtx = scanForCtx(vmFromVb, cls, 0)) != null) {
            return scanForCtx;
        }
        writeNow("diag_multi.txt", "[退路] 全部落空");
        return null;
    }

    private boolean hookBar(ClassLoader classLoader) {
        Method findMethodByType;
        Class<?> findClass = HookApi.findClass(classLoader, "com.tencent.tim.aio.inputbar.TimMultiSelectBarVB");
        if (findClass == null) {
            HookLog.w("多选撤回：未找到 com.tencent.tim.aio.inputbar.TimMultiSelectBarVB");
            writeNow("diag_multi.txt", "[挂载失败] 未找到 com.tencent.tim.aio.inputbar.TimMultiSelectBarVB");
            return false;
        }
        Class<?> findClass2 = HookApi.findClass(classLoader, "com.tencent.a0.a.c.a");
        Class<?> loadInner = loadInner(classLoader, "com.tencent.tim.aio.inputbar.TimMultiSelectBarVB", "$mOperationLayout$2");
        Method findMethodByType2 = loadInner != null ? Refl.findMethodByType(loadInner, View.class, new Class[0]) : null;
        if (findMethodByType2 == null && findClass2 != null) {
            findMethodByType2 = Refl.findMethodByType(findClass, View.class, findClass2);
        }
        if (findMethodByType2 == null) {
            Method[] declaredMethods = findClass.getDeclaredMethods();
            int length = declaredMethods.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Method method = declaredMethods[i];
                if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 1 && View.class.isAssignableFrom(method.getReturnType())) {
                    findMethodByType2 = method;
                    break;
                }
                i++;
            }
        }
        if (findMethodByType2 == null) {
            writeNow("diag_multi.txt", "[挂载失败] 未找到返回 View 的单参方法");
            return false;
        }
        String name = findMethodByType2.getName();
        writeNow("diag_multi.txt", "[挂载] 按钮锚点 = " + findMethodByType2.getDeclaringClass().getName() + "." + name + "(" + findMethodByType2.getParameterTypes().length + "参) : " + findMethodByType2.getReturnType().getSimpleName());
        hookAfter(findMethodByType2, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                if (FeatureUnlockMultiRecall.this.on()) {
                    try {
                        Object result = methodHookParam.getResult();
                        FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[J命中] 返回类型=" + (result == null ? "null" : result.getClass().getName()));
                        if (result instanceof View) {
                            View view = (View) result;
                            if (!(view instanceof LinearLayout)) {
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[跳过] 不是 LinearLayout 而是 " + view.getClass().getName());
                                return;
                            }
                            LinearLayout linearLayout = (LinearLayout) view;
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[操作栏] 子数=" + linearLayout.getChildCount() + " 已含撤回=" + (linearLayout.findViewById(FeatureUnlockMultiRecall.ID_RECALL_BTN) != null));
                            if (linearLayout.findViewById(FeatureUnlockMultiRecall.ID_RECALL_BTN) != null) {
                                return;
                            }
                            Object obj = null;
                            List unused = FeatureUnlockMultiRecall.sLastSelected = null;
                            synchronized (FeatureUnlockMultiRecall.SELECTED_MSGS) {
                                FeatureUnlockMultiRecall.SELECTED_MSGS.clear();
                            }
                            try {
                                obj = Refl.field(methodHookParam.thisObject, "this$0");
                                if (obj != null) {
                                    Object unused2 = FeatureUnlockMultiRecall.sBarVB = obj;
                                }
                            } catch (Throwable th) {
                            }
                            FeatureUnlockMultiRecall.this.addRecallButton(linearLayout, obj);
                        }
                    } catch (Throwable th2) {
                        HookLog.e("多选撤回：插按钮失败", th2);
                        FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[异常] " + th2);
                    }
                    FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[J结束] 撤回按钮挂载流程走完");
                }
            }
        });
        HookLog.i("多选撤回：已挂 " + name);
        try {
            Class<?> findClass3 = HookApi.findClass(classLoader, "com.tencent.a0.a.c.a");
            if (findClass3 != null && (findMethodByType = Refl.findMethodByType(findClass, View.class, findClass3)) != null && findMethodByType != findMethodByType2) {
                hookAfter(findMethodByType, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                        if (FeatureUnlockMultiRecall.this.on()) {
                            try {
                                Object result = methodHookParam.getResult();
                                if (!(result instanceof LinearLayout)) {
                                    FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[J(a)命中] 非LL: " + (result == null ? "null" : result.getClass().getName()));
                                    return;
                                }
                                LinearLayout linearLayout = (LinearLayout) result;
                                if (linearLayout.findViewById(FeatureUnlockMultiRecall.ID_RECALL_BTN) != null) {
                                    return;
                                }
                                Object obj = null;
                                try {
                                    obj = Refl.field(methodHookParam.thisObject, "this$0");
                                    if (obj == null) {
                                        obj = methodHookParam.thisObject;
                                    }
                                    Object unused = FeatureUnlockMultiRecall.sBarVB = obj;
                                } catch (Throwable th) {
                                }
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[J(a)命中] 子数=" + linearLayout.getChildCount());
                                FeatureUnlockMultiRecall.this.addRecallButton(linearLayout, obj);
                            } catch (Throwable th2) {
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[J(a)异常] " + th2);
                            }
                        }
                    }
                });
                writeNow("diag_multi.txt", "[补挂] " + findMethodByType.getName() + "(a) 成功");
            }
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[补挂J(a)失败] " + th);
        }
        return true;
    }

    private boolean hookBubblePool(ClassLoader classLoader) {
        Class<?> findClass = HookApi.findClass(classLoader, CLS_BUBBLE_VB);
        boolean z = false;
        if (findClass != null) {
            Method findMethodByParams = HookApi.findMethodByParams(findClass, Void.TYPE, Integer.TYPE, null, List.class, Bundle.class);
            if (findMethodByParams == null) {
                Method[] declaredMethods = findClass.getDeclaredMethods();
                int length = declaredMethods.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Method method = declaredMethods[i];
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 4 && parameterTypes[0] == Integer.TYPE && parameterTypes[2] == List.class && parameterTypes[3] == Bundle.class) {
                        findMethodByParams = method;
                        break;
                    }
                    i++;
                }
            }
            if (findMethodByParams != null) {
                String name = findMethodByParams.getName();
                hookBefore(findMethodByParams, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object obj;
                        try {
                            if (methodHookParam.args != null) {
                                Object[] objArr = methodHookParam.args;
                                int length2 = objArr.length;
                                for (int i2 = 0; i2 < length2; i2++) {
                                    obj = objArr[i2];
                                    if (obj != null && FeatureUnlockMultiRecall.CLS_MSG_ITEM.equals(obj.getClass().getName())) {
                                        break;
                                    }
                                }
                            }
                            obj = null;
                            if (obj == null) {
                                return;
                            }
                            Object field = Refl.field(obj, "msgId");
                            if (field instanceof Number) {
                                long longValue = ((Number) field).longValue();
                                if (longValue == 0) {
                                    return;
                                }
                                synchronized (FeatureUnlockMultiRecall.MSG_ITEM_POOL) {
                                    if (FeatureUnlockMultiRecall.MSG_ITEM_POOL.containsKey(Long.valueOf(longValue))) {
                                        return;
                                    }
                                    FeatureUnlockMultiRecall.putCapped(FeatureUnlockMultiRecall.MSG_ITEM_POOL, longValue, obj);
                                }
                            }
                        } catch (Throwable th) {
                        }
                    }
                });
                writeNow("diag_multi.txt", "[④ 气泡池] 已挂 " + name);
                z = true;
            }
        }
        if (!z) {
            writeNow("diag_multi.txt", "[④] 挂载失败");
        }
        return z;
    }

    private boolean hookForwardHelper(ClassLoader classLoader) {
        Method method;
        Class<?> findClass = HookApi.findClass(classLoader, CLS_FORWARD_HELPER);
        if (findClass == null) {
            writeNow("diag_multi.txt", "[★] 未找到 com.tencent.mobileqq.aio.helper.MultiForwardHelper");
            return false;
        }
        Method[] declaredMethods = findClass.getDeclaredMethods();
        int length = declaredMethods.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                method = null;
                break;
            }
            method = declaredMethods[i];
            if (method.getParameterTypes().length == 1 && "B".equals(method.getName())) {
                break;
            }
            i++;
        }
        if (method == null) {
            for (Method method2 : findClass.getDeclaredMethods()) {
                if (method2.getParameterTypes().length == 1 && method2.getReturnType() == Void.TYPE) {
                    String name = method2.getParameterTypes()[0].getName();
                    if (name.contains("MsgIntent") || name.contains("mvi.base.route")) {
                        method = method2;
                        break;
                    }
                }
            }
        }
        if (method == null) {
            writeNow("diag_multi.txt", "[★] 未找到 B(MsgIntent)");
            return false;
        }
        String name2 = method.getName();
        hookAfter(method, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                try {
                    Object field = Refl.field(methodHookParam.thisObject, "g");
                    if (field instanceof List) {
                        List list = (List) field;
                        List unused = FeatureUnlockMultiRecall.sLastSelected = new ArrayList(list);
                        FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[★ 选中列表] " + list.size() + " 条 (g字段)");
                    }
                } catch (Throwable th) {
                }
            }
        });
        writeNow("diag_multi.txt", "[★ 多选助手] 已挂 " + name2 + "(MsgIntent)");
        return true;
    }

    private boolean hookOneSelectCtor(ClassLoader classLoader, String str, final boolean z) {
        try {
            int i = 0;
            for (Constructor<?> constructor : Class.forName(str, false, classLoader).getDeclaredConstructors()) {
                hookAfter(constructor, new HookApi.Action() {
                    @Override
                    public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object obj;
                        try {
                            if (methodHookParam.args != null) {
                                Object[] objArr = methodHookParam.args;
                                int length = objArr.length;
                                for (int i2 = 0; i2 < length; i2++) {
                                    obj = objArr[i2];
                                    if (obj != null && FeatureUnlockMultiRecall.CLS_MSG_ITEM.equals(obj.getClass().getName())) {
                                        break;
                                    }
                                }
                            }
                            obj = null;
                            if (obj == null) {
                                return;
                            }
                            long msgIdOf = FeatureUnlockMultiRecall.this.msgIdOf(obj);
                            if (msgIdOf == 0) {
                                return;
                            }
                            synchronized (FeatureUnlockMultiRecall.SELECTED_MSGS) {
                                if (z) {
                                    FeatureUnlockMultiRecall.SELECTED_MSGS.put(Long.valueOf(msgIdOf), obj);
                                } else {
                                    FeatureUnlockMultiRecall.SELECTED_MSGS.remove(Long.valueOf(msgIdOf));
                                }
                            }
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[③ " + (z ? "选中" : "取消") + "] msgId=" + msgIdOf + " 当前=" + FeatureUnlockMultiRecall.SELECTED_MSGS.size());
                        } catch (Throwable th) {
                            FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[③ hook异常] " + th);
                        }
                    }
                });
                i++;
            }
            writeNow("diag_multi.txt", "[③ " + shortName(str) + "] 挂 " + i + " 个构造");
            return i > 0;
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[③ 类加载失败] " + str);
            return false;
        }
    }

    private boolean hookSelectEvents(ClassLoader classLoader) {
        return hookOneSelectCtor(classLoader, "com.tencent.mobileqq.aio.event.MultiSelectEvent$AddSelectMsg", true) || hookOneSelectCtor(classLoader, "com.tencent.mobileqq.aio.event.MultiSelectEvent$RemoveSelectMsg", false) || clearOnRemoveAll(classLoader, "com.tencent.mobileqq.aio.event.MultiSelectEvent$RemoveAllSelectMsg");
    }

    private boolean hookVmOnCreate(ClassLoader classLoader) {
        Class<?> cls;
        String[] strArr = {"com.tencent.mobileqq.aio.input.multiselect.b", CLS_VM_BASE};
        Class<?> findClass = HookApi.findClass(classLoader, CLS_AIO_CTX);
        writeNow("diag_multi.txt", "[① AIO上下文] " + (findClass == null ? "无" : findClass.getName()));
        if (findClass == null) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            String str = strArr[i];
            try {
                cls = Class.forName(str, false, classLoader);
            } catch (Throwable th) {
                cls = null;
            }
            if (cls == null) {
                writeNow("diag_multi.txt", "[① 类加载失败] " + str);
            } else {
                Method findMethodByType = Refl.findMethodByType(cls, Void.TYPE, findClass);
                writeNow("diag_multi.txt", "[① " + shortName(str) + ".onCreate] " + (findMethodByType == null ? "未找到" : findMethodByType.getName()));
                if (findMethodByType != null) {
                    String name = findMethodByType.getName();
                    hookAfter(findMethodByType, new HookApi.Action() {
                        @Override
                        public void call(XC_MethodHook.MethodHookParam methodHookParam) {
                            try {
                                if (methodHookParam.args == null || methodHookParam.args.length <= 0 || methodHookParam.args[0] == null) {
                                    return;
                                }
                                Object unused = FeatureUnlockMultiRecall.sAioCtx = methodHookParam.args[0];
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[① 抓到上下文] " + methodHookParam.args[0].getClass().getName());
                            } catch (Throwable th2) {
                                FeatureUnlockMultiRecall.this.writeNow("diag_multi.txt", "[① 缓存异常] " + th2);
                            }
                        }
                    });
                    HookLog.i("多选撤回：已挂 " + shortName(str) + "." + name);
                    MsgStore.add("[多选撤回] 已挂 " + shortName(str) + "." + name);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isItemSelected(Object obj, Object obj2, Object obj3) {
        try {
            cls(CLS_MSG_ITEM);
            for (Method method : obj.getClass().getDeclaredMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2 && method.getReturnType() == Boolean.TYPE && parameterTypes[0].isInstance(obj2) && parameterTypes[1].isInstance(obj3)) {
                    method.setAccessible(true);
                    Object invoke = method.invoke(obj, obj2, obj3);
                    if ((invoke instanceof Boolean) && ((Boolean) invoke).booleanValue()) {
                        return true;
                    }
                }
            }
        } catch (Throwable th) {
        }
        return false;
    }

    private boolean isVmLike(Object obj) {
        String name = obj.getClass().getName();
        if (name.startsWith("android.") || name.startsWith("java.") || name.startsWith("kotlin.") || (obj instanceof Context) || (obj instanceof View)) {
            return false;
        }
        return name.contains("VM") || name.contains("mvvm") || name.contains("multiselect");
    }

    public static List<?> lastSelectedSnapshot() {
        return sLastSelected;
    }

    private Class<?> loadInner(ClassLoader classLoader, String str, String str2) {
        String str3 = str + str2;
        try {
            return Class.forName(str3, false, classLoader);
        } catch (Throwable th) {
            try {
                return Class.forName(str3);
            } catch (Throwable th2) {
                writeNow("diag_multi.txt", "[内部类加载失败] " + str3);
                return null;
            }
        }
    }

    public long msgIdOf(Object obj) {
        Object obj2;
        try {
            Method method = obj.getClass().getMethod("getMsgRecord", new Class[0]);
            method.setAccessible(true);
            obj2 = method.invoke(obj, new Object[0]);
        } catch (Throwable th) {
            obj2 = null;
        }
        if (obj2 != null) {
            try {
                Object field = Refl.field(obj2, "msgId");
                if (field instanceof Number) {
                    return ((Number) field).longValue();
                }
            } catch (Throwable th2) {
                return 0L;
            }
        }
        Object field2 = Refl.field(obj, "msgId");
        if (field2 instanceof Number) {
            return ((Number) field2).longValue();
        }
        return 0L;
    }

    public boolean on() {
        return this.ctx != null && on(this.ctx);
    }

    public static boolean on(Context context) {
        return HookConfig.getBoolean(context, K, false);
    }

    private List<?> pullFromKernel() {
        try {
            Object currentContact = currentContact();
            if (currentContact == null) {
                writeNow("diag_multi.txt", "[内核拉取] 拿不到 Contact");
                return null;
            }
            writeNow("diag_multi.txt", "[内核拉取] Contact = " + currentContact);
            List<?> msgsBySeqAndCount = NtMsgApi.getMsgsBySeqAndCount(currentContact, 4611686018427387903L, 50);
            writeNow("diag_multi.txt", "[内核拉取] = " + (msgsBySeqAndCount == null ? "null" : msgsBySeqAndCount.size() + " 条"));
            return msgsBySeqAndCount;
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[内核拉取异常] " + th);
            return null;
        }
    }

    private List<?> pullRecentMsgs(Object obj) {
        Object next;
        try {
            LinkedHashMap<Long, Object> linkedHashMap = MSG_ITEM_POOL;
            synchronized (linkedHashMap) {
                Iterator<Object> it = linkedHashMap.values().iterator();
                next = it.hasNext() ? it.next() : null;
            }
            writeNow("diag_multi.txt", "[拉消息] 池里有 " + (next == null ? 0 : linkedHashMap.size()) + " 条可取样");
            if (next != null) {
                Object recordOf = getRecordOf(next);
                writeNow("diag_multi.txt", "[拉消息] 取样记录 = " + (recordOf == null ? "null" : recordOf.getClass().getSimpleName()));
            }
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[拉消息异常] " + th);
        }
        return pullFromKernel();
    }

    public static void putCapped(LinkedHashMap<Long, Object> linkedHashMap, long j, Object obj) {
        linkedHashMap.put(Long.valueOf(j), obj);
        while (linkedHashMap.size() > 600) {
            Iterator<Long> it = linkedHashMap.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    private List<?> readVmSelectedList() {
        Object vmFromVb;
        Object obj = sBarVB;
        if (obj == null) {
            return null;
        }
        try {
            vmFromVb = getVmFromVb(obj, null);
        } catch (Throwable th) {
            vmFromVb = null;
            writeNow("diag_multi.txt", "[读VM字段异常] " + th);
        }
        if (vmFromVb == null) {
            return null;
        }
        Class<?> cls = vmFromVb.getClass();
        for (int i = 0; cls != null && i < 5; i++) {
            for (Field field : cls.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object obj2 = field.get(vmFromVb);
                        if ((obj2 instanceof List) && !((List) obj2).isEmpty()) {
                            writeNow("diag_multi.txt", "[VM字段命中] " + cls.getSimpleName() + "." + field.getName() + " = " + ((List) obj2).size() + " 条, 元素类型=" + ((List) obj2).get(0).getClass().getName());
                            return (List) obj2;
                        }
                    } catch (Throwable th2) {
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private Object recordOf(Object obj) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod("getMsgRecord", new Class[0]);
            m.setAccessible(true);
            Object r = m.invoke(obj, new Object[0]);
            if (r != null) return r;
        } catch (Throwable ignore) {
        }
        return obj;
    }

    private long msgIdOf2(Object rec) {
        try {
            if (rec == null) return 0L;
            Method m = rec.getClass().getMethod("getMsgId", new Class[0]);
            m.setAccessible(true);
            Object v = m.invoke(rec, new Object[0]);
            if (v instanceof Number) return ((Number) v).longValue();
        } catch (Throwable ignore) {
        }
        Object f = Refl.field(rec, "msgId");
        return f instanceof Number ? ((Number) f).longValue() : 0L;
    }

    private int chatTypeOf(Object rec) {
        try {
            Method m = rec.getClass().getMethod("getChatType", new Class[0]);
            m.setAccessible(true);
            Object v = m.invoke(rec, new Object[0]);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Throwable ignore) {
        }
        return Refl.i32(Refl.field(rec, "chatType"), 0);
    }

    private String peerUidOf(Object rec) {
        try {
            Method m = rec.getClass().getMethod("getPeerUid", new Class[0]);
            m.setAccessible(true);
            Object v = m.invoke(rec, new Object[0]);
            if (v instanceof String) return (String) v;
        } catch (Throwable ignore) {
        }
        return Refl.str(Refl.field(rec, "peerUid"));
    }

    private String guildIdOf(Object rec) {
        try {
            Method m = rec.getClass().getMethod("getGuildId", new Class[0]);
            m.setAccessible(true);
            Object v = m.invoke(rec, new Object[0]);
            if (v instanceof String) return (String) v;
        } catch (Throwable ignore) {
        }
        String s = Refl.str(Refl.field(rec, "guildId"));
        return s == null ? "" : s;
    }

    private int fireRecallBurst(Object contact, ArrayList<Long> ids) {
        if (contact == null || ids == null || ids.isEmpty()) return -1;
        StringBuilder trace = new StringBuilder("[连发] ids=" + ids.size());

        try {
            Object impl = QQEnv.msgServiceImpl();
            Method hit = null;
            for (Method m : impl.getClass().getMethods()) {
                if (!"recallMsg".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && List.class.isAssignableFrom(p[1])) { hit = m; break; }
            }
            if (hit != null) {
                hit.setAccessible(true);
                int code = invokeWithCallback(impl, hit, contact, ids, trace, "①impl");
                if (code != Integer.MIN_VALUE) {
                    writeNow("diag_multi.txt", trace.toString());
                    return code;
                }
            } else {
                trace.append(" | ①无三参批量");
            }
        } catch (Throwable t) {
            trace.append(" | ①异常 ").append(t);
        }

        try {
            Object svc = QQEnv.kernelMsgService();
            if (svc != null) {
                Method hit = null;
                for (Method m : svc.getClass().getMethods()) {
                    if (!"recallMsg".equals(m.getName())) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length >= 2 && List.class.isAssignableFrom(p[1])) {
                        if (hit == null || p.length < hit.getParameterTypes().length) hit = m;
                    }
                }
                if (hit != null) {
                    hit.setAccessible(true);
                    int code = invokeWithCallback(svc, hit, contact, ids, trace, "②kernel");
                    if (code != Integer.MIN_VALUE) {
                        writeNow("diag_multi.txt", trace.toString());
                        return code;
                    }
                }
            }
            trace.append(" | ②无批量");
        } catch (Throwable t) {
            trace.append(" | ②异常 ").append(t);
        }

        writeNow("diag_multi.txt", trace.toString());
        return -1;
    }

    private int invokeWithCallback(Object target, Method m, Object contact,
                                   ArrayList<Long> ids, StringBuilder trace, String tag) {
        try {
            Class<?>[] ps = m.getParameterTypes();

            if (ps.length < 3) {
                if (ps.length == 2) m.invoke(target, contact, ids);
                else return Integer.MIN_VALUE;
                trace.append(" | ").append(tag).append(" 无回调，按已提交");
                return Integer.MIN_VALUE;
            }

            final int[] code = new int[]{Integer.MIN_VALUE};
            final String[] err = new String[]{""};
            final java.util.concurrent.CountDownLatch latch =
                    new java.util.concurrent.CountDownLatch(1);
            Class<?> cbCls = ps[2];
            Object cb = null;
            try {
                cb = java.lang.reflect.Proxy.newProxyInstance(
                        cl, new Class<?>[]{cbCls}, new java.lang.reflect.InvocationHandler() {
                            @Override public Object invoke(Object p, Method mm, Object[] a) {
                                String n = mm.getName();
                                try {
                                    if (a != null && a.length >= 1 && a[0] instanceof Integer) {
                                        code[0] = (Integer) a[0];
                                        if (a.length >= 2 && a[1] != null) err[0] = String.valueOf(a[1]);
                                        latch.countDown();
                                    } else if ("onResult".equals(n) && a != null) {
                                        latch.countDown();
                                    }
                                } catch (Throwable ignore) {
                                }
                                return null;
                            }
                        });
            } catch (Throwable t) {
                trace.append(" | ").append(tag).append(" 回调接口建不出来 ").append(t);
            }

            m.invoke(target, contact, ids, cb);
            if (cb != null) {
                latch.await(6000, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            trace.append(" | ").append(tag).append(" code=").append(code[0])
                    .append(" err=").append(err[0]);
            if (code[0] == Integer.MIN_VALUE) {
                trace.append("（等不到回执）");
                return Integer.MIN_VALUE;
            }
            return code[0];
        } catch (Throwable t) {
            trace.append(" | ").append(tag).append(" 调用异常 ").append(t);
            return Integer.MIN_VALUE;
        }
    }

    public boolean recallOne(Object obj) throws Throwable {        if (obj == null) {
            return false;
        }
        try {
            Method method = obj.getClass().getMethod("getMsgRecord", new Class[0]);
            method.setAccessible(true);
            obj = method.invoke(obj, new Object[0]);
        } catch (Throwable th) {
        }
        if (obj == null) {
            return false;
        }
        return MsgApi.recall(obj);
    }

    private Drawable rippleBg() {
        try {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            gradientDrawable.setColor(-1);
            return new RippleDrawable(ColorStateList.valueOf(864059520), null, gradientDrawable);
        } catch (Throwable th) {
            return null;
        }
    }

    private Object scanForCtx(Object obj, Class<?> cls, int i) {
        Object scanForCtx;
        if (obj == null || i > 4) {
            return null;
        }
        if (cls.isInstance(obj)) {
            return obj;
        }
        Class<?> cls2 = obj.getClass();
        for (int i2 = 0; cls2 != null && i2 < 6; i2++) {
            for (Field field : cls2.getDeclaredFields()) {
                try {
                    if (cls.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object obj2 = field.get(obj);
                        if (obj2 != null) {
                            writeNow("diag_multi.txt", "[退路·上下文命中] " + cls2.getSimpleName() + "." + field.getName());
                            return obj2;
                        }
                        continue;
                    }
                } catch (Throwable th) {
                }
            }
            cls2 = cls2.getSuperclass();
        }
        Class<?> cls3 = obj.getClass();
        for (int i3 = 0; cls3 != null && i3 < 4; i3++) {
            for (Field field2 : cls3.getDeclaredFields()) {
                if (!field2.getType().isPrimitive()) {
                    String name = field2.getType().getName();
                    if (!name.startsWith("java.") && !name.startsWith("android.") && !name.startsWith("kotlin.")) {
                        try {
                            field2.setAccessible(true);
                            Object obj3 = field2.get(obj);
                            if (obj3 != null && (scanForCtx = scanForCtx(obj3, cls, i + 1)) != null) {
                                return scanForCtx;
                            }
                        } catch (Throwable th2) {
                        }
                    }
                }
            }
            cls3 = cls3.getSuperclass();
        }
        return null;
    }

    private static String shortName(String str) {
        if (str == null) {
            return "?";
        }
        int lastIndexOf = str.lastIndexOf(46);
        return lastIndexOf < 0 ? str : str.substring(lastIndexOf + 1);
    }

    private Object toMsgItem(Object obj) {
        Class<?> cls = null;
        int i;
        if (obj == null) {
            return null;
        }
        try {
            cls = cls(CLS_MSG_ITEM);
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[转AIOMsgItem异常] " + th);
        }
        if (cls == null) {
            return null;
        }
        if (cls.isInstance(obj)) {
            return obj;
        }
        Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
        int length = declaredConstructors.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            Constructor<?> constructor = declaredConstructors[i2];
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length >= 1 && parameterTypes[0].isInstance(obj)) {
                try {
                    constructor.setAccessible(true);
                    Object[] objArr = new Object[parameterTypes.length];
                    objArr[0] = obj;
                    for (int i3 = 1; i3 < parameterTypes.length; i3++) {
                        objArr[i3] = parameterTypes[i3] == Integer.TYPE ? 0 : parameterTypes[i3] == Boolean.TYPE ? Boolean.FALSE : null;
                    }
                    return constructor.newInstance(objArr);
                } catch (Throwable th2) {
                }
            }
            i2++;
        }
        for (Method method : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                Class<?>[] parameterTypes2 = method.getParameterTypes();
                if (parameterTypes2.length == 1 && parameterTypes2[0].isInstance(obj) && cls.isAssignableFrom(method.getReturnType())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(null, obj);
                    } catch (Throwable th3) {
                    }
                }
            }
        }
        writeNow("diag_multi.txt", "[转AIOMsgItem] 没找到构造/工厂");
        return null;
    }

    private void toast(String str) {
        toastOnUi(str);
    }

    public void toastOnUi(final String str) {
        try {
            QQUtils.runOnUi(new Runnable() {
                @Override
                public void run() {
                    try {
                        UiKit.catToast(FeatureUnlockMultiRecall.this.ctx, str);
                    } catch (Throwable th) {
                    }
                }
            });
        } catch (Throwable th) {
        }
    }

    private boolean triggerNativeRecall() {
        Object obj;
        final Object obj2;
        Object obj3 = sBarVB;
        if (obj3 == null) {
            return false;
        }
        Class<?> cls = null;
        final Object vmFromVb = getVmFromVb(obj3, null);
        if (vmFromVb == null) {
            writeNow("diag_multi.txt", "[原生撤回] 没拿到 VM");
            return false;
        }
        try {
            Class<?> cls2 = ClassResolver.find(this.cl, "aio.multiselect.c$c",
                    "com.tencent.mobileqq.aio.input.multiselect.c$c");
            int i = 1;
            try {
                Field declaredField = cls2.getDeclaredField("d");
                declaredField.setAccessible(true);
                obj = declaredField.get(null);
                if (obj == null) {
                    obj = null;
                }
            } catch (Throwable th) {
                obj = null;
            }
            if (obj == null) {
                Constructor<?> declaredConstructor = cls2.getDeclaredConstructor(new Class[0]);
                declaredConstructor.setAccessible(true);
                obj2 = declaredConstructor.newInstance(new Object[0]);
            } else {
                obj2 = obj;
            }
            writeNow("diag_multi.txt", "[原生撤回] 意图 = " + obj2.getClass().getName());
            Class<?> cls3 = vmFromVb.getClass();
            Method method = null;
            int i2 = 0;
            while (cls3 != null && i2 < 8) {
                Method[] declaredMethods = cls3.getDeclaredMethods();
                int length = declaredMethods.length;
                int i3 = 0;
                while (i3 < length) {
                    Method method2 = declaredMethods[i3];
                    if (method2.getParameterTypes().length == i && method2.getReturnType() == Void.TYPE) {
                        Class<?> cls4 = method2.getParameterTypes()[0];
                        if (cls4.isAssignableFrom(cls2) && (cls == null || cls.isAssignableFrom(cls4))) {
                            cls = cls4;
                            method = method2;
                        }
                    }
                    i3++;
                    i = 1;
                }
                cls3 = cls3.getSuperclass();
                i2++;
                i = 1;
            }
            if (method == null) {
                writeNow("diag_multi.txt", "[原生撤回] 没找到分发方法");
                return false;
            }
            writeNow("diag_multi.txt", "[原生撤回] 分发方法 = " + method.getName() + "(" + method.getParameterTypes()[0].getSimpleName() + ") 声明类=" + method.getDeclaringClass().getSimpleName());
            method.setAccessible(true);
            final Throwable[] thArr = new Throwable[1];
            final Method method3 = method;
            QQUtils.runOnUi(new Runnable() {
                @Override
                public void run() {
                    try {
                        method3.invoke(vmFromVb, obj2);
                    } catch (Throwable th2) {
                        thArr[0] = th2;
                    }
                }
            });
            Throwable th2 = thArr[0];
            if (th2 == null) {
                writeNow("diag_multi.txt", "[原生撤回] 已调用 " + method.getName() + "(意图)");
                return true;
            }
            if ((th2 instanceof InvocationTargetException) && ((InvocationTargetException) th2).getCause() != null) {
                th2 = ((InvocationTargetException) th2).getCause();
            }
            StackTraceElement[] stackTrace = th2.getStackTrace();
            StringBuilder sb = new StringBuilder("[原生撤回·UI线程异常] " + th2 + " | ");
            for (int i4 = 0; i4 < Math.min(stackTrace.length, 6); i4++) {
                sb.append(stackTrace[i4].getClassName()).append('.').append(stackTrace[i4].getMethodName()).append(':').append(stackTrace[i4].getLineNumber()).append(" | ");
            }
            writeNow("diag_multi.txt", sb.toString());
            return false;
        } catch (Throwable th3) {
            Throwable th = th3;
            if (th instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) th;
                if (invocationTargetException.getCause() != null) {
                    th = invocationTargetException.getCause();
                }
            }
            writeNow("diag_multi.txt", "[原生撤回异常] " + th);
            StackTraceElement[] stackTrace2 = th.getStackTrace();
            StringBuilder sb2 = new StringBuilder("[原生撤回栈] ");
            for (int i5 = 0; i5 < Math.min(stackTrace2.length, 6); i5++) {
                sb2.append(stackTrace2[i5].getClassName()).append('.').append(stackTrace2[i5].getMethodName()).append(':').append(stackTrace2[i5].getLineNumber()).append(" | ");
            }
            writeNow("diag_multi.txt", sb2.toString());
            return false;
        }
    }

    private Object vbInObject(Object obj, int i) {
        if (obj == null || i > 3) {
            return null;
        }
        if (obj.getClass().getName().contains("MultiSelectBarVB")) {
            return obj;
        }
        Class<?> cls = obj.getClass();
        for (int i2 = 0; cls != null && i2 < 4; i2++) {
            for (Field field : cls.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object obj2 = field.get(obj);
                    if (obj2 != null && obj2.getClass().getName().contains("MultiSelectBarVB")) {
                        return obj2;
                    }
                } catch (Throwable th) {
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private boolean verifySelectionFresh(List<Object> list) {
        Class<?> cls;
        try {
            Object obj = null;
            Object vmFromVb = getVmFromVb(sBarVB, null);
            Object ctxFromVm = (vmFromVb == null || (cls = cls(CLS_AIO_CTX)) == null) ? null : getCtxFromVm(vmFromVb, cls);
            if (ctxFromVm == null) {
                ctxFromVm = grabAioCtxNow(sBarVB);
            }
            if (ctxFromVm == null) {
                return true;
            }
            try {
                Field declaredField = cls(CLS_MULTI_UTIL).getDeclaredField("a");
                declaredField.setAccessible(true);
                obj = declaredField.get(null);
            } catch (Throwable th) {
            }
            if (obj == null) {
                return true;
            }
            return isItemSelected(obj, ctxFromVm, list.get(0));
        } catch (Throwable th2) {
            writeNow("diag_multi.txt", "[校验异常] " + th2);
            return true;
        }
    }

    public void writeNow(String str, String str2) {
        Context appContext;
        try {
            File filesDir = this.ctx != null ? this.ctx.getFilesDir() : null;
            if (filesDir == null && (appContext = QQEnv.appContext()) != null) {
                filesDir = appContext.getFilesDir();
            }
            if (filesDir == null) {
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(new File(filesDir, str), true);
            fileOutputStream.write(("\n===== " + new Date() + " =====\n").getBytes("UTF-8"));
            fileOutputStream.write(str2.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Throwable th) {
        }
    }

    @Override
    public void entry(ClassLoader classLoader) {
        hint("进入多选模式时");
        noInstrument();

        try {
            File f = new File(ctx.getFilesDir(), "diag_multi.txt");
            if (f.exists()) f.delete();
        } catch (Throwable ignore) {
        }
        writeNow("diag_multi.txt", "[entry 进入] cl=" + (classLoader == null ? "null" : "ok") + " ver=V2连发");
        int i = 0;
        try {
            i = 0 + (hookForwardHelper(classLoader) ? 1 : 0);
        } catch (Throwable th) {
            writeNow("diag_multi.txt", "[★异常] " + th);
        }
        try {
            i += hookBar(classLoader) ? 1 : 0;
        } catch (Throwable th2) {
            writeNow("diag_multi.txt", "[②异常] " + th2);
        }
        HookLog.i("多选撤回：安装完成 " + i + "/2");
        MsgStore.add("[多选撤回] 安装完成 " + i + "/2");
    }

    @Override
    public String switchKey() {
        return K;
    }
}
