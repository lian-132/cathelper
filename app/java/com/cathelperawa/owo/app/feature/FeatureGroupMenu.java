package com.cathelperawa.owo.app.feature;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.R;
import com.cathelperawa.owo.app.api.GroupManageApi;
import com.cathelperawa.owo.app.api.IdentityApi;
import com.cathelperawa.owo.app.api.PanelApi;
import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.core.HookApi;
import com.cathelperawa.owo.app.core.HookItem;
import com.cathelperawa.owo.app.core.HookItemBase;
import com.cathelperawa.owo.app.core.HookLog;
import com.cathelperawa.owo.app.util.Refl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

@HookItem(name = "群主便捷菜单", path = "群管理/长按头像",
        description = "长按群成员头像弹出管理面板", order = 90)
public class FeatureGroupMenu extends HookItemBase {

    private static final String CLS_BUBBLE_VB =
            "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB";

    private static final java.util.Map<View, Boolean> HOOKED =
            java.util.Collections.synchronizedMap(
                    new java.util.WeakHashMap<View, Boolean>());

    private static final java.util.Map<View, Object> ORIG =
            java.util.Collections.synchronizedMap(
                    new java.util.WeakHashMap<View, Object>());

    public boolean mainProcessOnly() { return true; }

    public static final String K_EXTRACT = "group_menu_extract";

    public static boolean extractOnly(android.content.Context c) {
        try {
            return com.cathelperawa.owo.app.core.HookConfig.getBoolean(c, K_EXTRACT, false)
                    || com.cathelperawa.owo.app.core.HookConfig.getBoolean(c,
                            com.cathelperawa.owo.app.ui.GroupPage.K_EXTRACT, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String switchKey() {
        return "group_owner_menu";
    }

    @Override
    public void entry(ClassLoader cl) {
        hint("长按群头像时");

        Class<?> vb = HookApi.findClass(cl, CLS_BUBBLE_VB);
        if (vb == null) {
            fail("未找到 AIOBubbleMsgItemVB");
            return;
        }
        Method target = null;
        for (Method m : vb.getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (m.getReturnType() != void.class) continue;
            if (ps.length == 4 && ps[0] == int.class
                    && ps[2] == List.class && ps[3] == Bundle.class) {
                target = m;
                break;
            }
        }
        if (target == null) {
            fail("未找到气泡绑定方法");
            return;
        }

        hookAfter(target, new HookApi.Action() {
            @Override
            public void call(XC_MethodHook.MethodHookParam p) {
                try {
                    if (!isEnabled()) return;
                    if (p.args == null || p.args.length < 2) return;
                    final Object msgItem = p.args[1];
                    if (msgItem == null) return;

                    final View root = findViewField(p.thisObject);
                    if (root == null) return;

                    root.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                attach(root, msgItem);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                } catch (Throwable ignore) {
                    
                }
            }
        });
        HookLog.i("群主菜单：已挂 " + target.getName());
    }

    

    
    @Override
    protected void check(com.cathelperawa.owo.app.core.SelfCheckResult.Checker c) {
        Class<?> vb = c.cls("气泡视图 VB",
                "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB");
        boolean hasBind = false;
        if (vb != null) {
            for (Method m : vb.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getReturnType() == void.class && ps.length == 4
                        && ps[0] == int.class && ps[2] == List.class) {
                    hasBind = true;
                    break;
                }
            }
        }
        c.assertThat("气泡绑定方法", hasBind, hasBind ? "存在" : "找不到 (int, MsgItem, List, Bundle)");
        c.cls("群信息服务", "com.tencent.mobileqq.troop.api.ITroopInfoService");
    }

    private void attach(View root, final Object msgItem) {

        
        Object rec = null;
        try {
            rec = Refl.call(msgItem, "getMsgRecord");
            if (rec == null) rec = Refl.field(msgItem, "msgRecord");
        } catch (Throwable ignore) {
        }
        String peer = Refl.firstStrOr(rec, "", "peerUid", "peerUin");
        if (peer == null || peer.isEmpty() || peer.startsWith("u_")) return;
        
        final boolean onlyExtract = extractOnly(
                com.cathelperawa.owo.app.bridge.QQEnv.appContext());
        if (!onlyExtract && !isOwnerOrAdmin(peer)) return;

        final View avatar = findAvatar(root);
        if (avatar == null) return;
        if (HOOKED.containsKey(avatar)) return;
        HOOKED.put(avatar, Boolean.TRUE);

        final Object origTouch = Refl.field(avatar, "mOnTouchListener");

        avatar.setOnTouchListener(new View.OnTouchListener() {
            private long downAt;
            private float downX, downY;
            private boolean fired;
            private Runnable pending;

            private void cancelPending(View v) {
                if (pending != null) {
                    v.removeCallbacks(pending);
                    pending = null;
                }
            }

            private void firePanel(final View v) {
                if (fired) return;
                fired = true;
                final View fv = v;
                v.post(new Runnable() {
                    @Override public void run() {
                        try {
                            fire("长按头像");
                            showPanel(fv.getContext(), msgItem, fv);
                        } catch (Throwable t) {
                            HookLog.e("群主菜单弹出失败", t);
                        }
                    }
                });
            }

            @Override
            public boolean onTouch(View v, final android.view.MotionEvent e) {
                int a = e.getActionMasked();
                if (a == android.view.MotionEvent.ACTION_DOWN) {
                    downAt = android.os.SystemClock.uptimeMillis();
                    downX = e.getX();
                    downY = e.getY();
                    fired = false;
                    cancelPending(v);

                    
                    
                    pending = new Runnable() {
                        @Override public void run() {
                            pending = null;
                            firePanel(v);
                        }
                    };
                    v.postDelayed(pending, 500);

                } else if (a == android.view.MotionEvent.ACTION_MOVE) {
                    if (Math.abs(e.getX() - downX) > 26 || Math.abs(e.getY() - downY) > 26) {
                        cancelPending(v);   
                    }
                } else if (a == android.view.MotionEvent.ACTION_UP
                        || a == android.view.MotionEvent.ACTION_CANCEL) {
                    cancelPending(v);
                    
                    long dt = android.os.SystemClock.uptimeMillis() - downAt;
                    if (dt > 450 && Math.abs(e.getX() - downX) < 26
                            && Math.abs(e.getY() - downY) < 26) {
                        firePanel(v);
                    }
                }

                
                if (a == android.view.MotionEvent.ACTION_UP && !fired) {
                    long dt = android.os.SystemClock.uptimeMillis() - downAt;
                    if (dt < 450 && Math.abs(e.getX() - downX) < 26
                            && Math.abs(e.getY() - downY) < 26) {
                        try {
                            v.performClick();
                        } catch (Throwable ignore) {
                        }
                    }
                }
                return true;
            }
        });
    }

    private static void clearAllPressed() {
        try {
            for (View v : HOOKED.keySet()) {
                if (v != null) clearPressed(v);
            }
        } catch (Throwable ignore) {
        }
    }

    private static void clearPressed(View v) {
        try {
            v.setPressed(false);
            v.cancelLongPress();
            v.clearFocus();
        } catch (Throwable ignore) {
        }
    }

    private static View findAvatar(View root) {
        try {
            String pkg = root.getContext().getPackageName();
            String[] names = new String[]{"avatar", "head", "troop_avatar", "qq_head"};
            for (int i = 0; i < names.length; i++) {
                int id = root.getResources().getIdentifier(names[i], "id", pkg);
                if (id == 0) continue;
                View v = root.findViewById(id);
                if (v instanceof ImageView) return v;
            }
        } catch (Throwable ignore) {
        }

        List<View> all = new ArrayList<View>();
        collect(root, all, 0);
        float density = root.getResources().getDisplayMetrics().density;
        for (int i = 0; i < all.size(); i++) {
            View v = all.get(i);
            if (!(v instanceof ImageView)) continue;
            int w = v.getWidth();
            int h = v.getHeight();
            if (w <= 0 || h <= 0) continue;
            if (Math.abs(w - h) > w * 0.15) continue;      
            float dp = w / density;
            if (dp >= 28 && dp <= 64) return v;
        }
        return null;
    }

    private static void collect(View v, List<View> out, int depth) {
        if (v == null || depth > 12 || out.size() > 80) return;
        out.add(v);
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            collect(g.getChildAt(i), out, depth + 1);
        }
    }

    private static View findViewField(Object holder) {
        if (holder == null) return null;
        if (holder instanceof View) return (View) holder;
        Class<?> c = holder.getClass();
        int depth = 0;
        while (c != null && c != Object.class && depth++ < 8) {
            java.lang.reflect.Field[] fs = c.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                if (!View.class.isAssignableFrom(fs[i].getType())) continue;
                try {
                    fs[i].setAccessible(true);
                    Object o = fs[i].get(holder);
                    if (o instanceof View) return (View) o;
                } catch (Throwable ignore) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    

    private void showPanel(final Context ctx, Object msgItem, View avatarView) {
        Object rec = Refl.call(msgItem, "getMsgRecord");
        if (rec == null) rec = Refl.field(msgItem, "msgRecord");

        final String uid = Refl.firstStrOr(rec, "",
                "senderUid", "senderUin", "fromUid");
        String nickTmp = Refl.firstStrOr(rec, "",
                "sendNickName", "sendMemberName", "senderName");
        final String peer = Refl.firstStrOr(rec, "", "peerUid", "peerUin");
        String text = "";
        try {
            text = FeatureRecorder.extractText(rec);
        } catch (Throwable ignore) {
        }

        if (nickTmp == null || nickTmp.trim().isEmpty()) {
            try {
                nickTmp = IdentityApi.troopMemberName(peer, uid);
            } catch (Throwable ignore) {
            }
        }
        final String nick = (nickTmp == null || nickTmp.trim().isEmpty())
                ? "(未知)" : nickTmp;

        final String safeUid = uid == null ? "" : uid;
        final String safePeer = peer == null ? "" : peer;
        final String msgText = text == null ? "" : text;

        final boolean onlyExtract = extractOnly(ctx);
        final android.app.Dialog d = new android.app.Dialog(ctx,
                android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);

        
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3FFF6FA, 0xA6FFFFFF});
        float r = dp(ctx, 26);
        bg.setCornerRadius(r);
        bg.setStroke(Math.max(1, dp(ctx, 1)), 0x66FFD9E8);
        root.setBackgroundDrawable(bg);
        root.setPadding(dp(ctx, 22), dp(ctx, 22), dp(ctx, 22), dp(ctx, 26));

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        android.widget.ImageView av = new android.widget.ImageView(ctx);
        av.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        Bitmap bmp = drawableToBitmap(avatarView);
        int sz = dp(ctx, 64);
        if (bmp != null) {
            av.setImageBitmap(circleBitmap(bmp, sz));
        } else {
            
            TextView ph = new TextView(ctx);
            ph.setText(nick.substring(0, 1));
            ph.setTextSize(20);
            ph.setTextColor(0xFFFFFFFF);
            ph.setGravity(Gravity.CENTER);
            GradientDrawable pb = new GradientDrawable();
            pb.setShape(GradientDrawable.OVAL);
            pb.setColor(0xFF9EC6F5);
            ph.setBackgroundDrawable(pb);
            head.addView(ph, new LinearLayout.LayoutParams(sz, sz));
        }
        if (bmp != null) {
            head.addView(av, new LinearLayout.LayoutParams(sz, sz));
        }

        LinearLayout nameBox = new LinearLayout(ctx);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.setPadding(dp(ctx, 15), 0, 0, 0);
        nameBox.addView(bigText(ctx, nick, 19, 0xFF2E3A4D));
        nameBox.addView(bigText(ctx, "群号 " + safePeer, 12, 0xFF8A9BB0));
        nameBox.addView(bigText(ctx, "UID " + safeUid, 11, 0xFF9AA8BA));
        head.addView(nameBox, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView atBtn = bigText(ctx, "@TA", 11, 0xFF7C8DB5);
        atBtn.setPadding(dp(ctx, 9), dp(ctx, 7), dp(ctx, 9), dp(ctx, 7));
        GradientDrawable atBg = new GradientDrawable();
        atBg.setCornerRadius(dp(ctx, 10));
        atBg.setColor(0x1F7C8DB5);
        atBtn.setBackgroundDrawable(atBg);
        atBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean ok = insertAt(ctx, nick);
                toastSafe(ctx, ok ? ("已插入 @" + nick) : "找不到输入框");
                d.dismiss();
            }
        });
        head.addView(atBtn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView close = bigText(ctx, "✕", 19, 0xFFB6C1D0);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                clearAllPressed();
                d.dismiss();
            }
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36)));

        root.addView(head, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(divider(ctx));

        root.addView(sectionTitle(ctx, "操作区"));

        LinearLayout row1 = new LinearLayout(ctx);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(iconAction(ctx, R.drawable.ic_a_copy, "复制 UID", 0xFF6B7A8F,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        UiApiCopy(ctx, safeUid);
                        toastSafe(ctx, "已复制 UID");
                        d.dismiss();
                    }
                }), weight());
        if (!onlyExtract) {
        row1.addView(iconAction(ctx, R.drawable.ic_a_msg, "改群名片", 0xFF6B7A8F,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        d.dismiss();
                        chooseCardName(ctx, safePeer, safeUid, nick);
                    }
                }), weight()); }

        if (!onlyExtract) {
        row1.addView(iconAction(ctx, R.drawable.ic_a_recall, "撤回消息", 0xFF6B7A8F,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        boolean ok = false;
                        try {
                            Object rec2 = Refl.call(msgItem, "getMsgRecord");
                            if (rec2 == null) rec2 = Refl.field(msgItem, "msgRecord");
                            ok = rec2 != null
                                    && com.cathelperawa.owo.app.api.CatApi.recall(rec2);
                        } catch (Throwable ignore) {
                        }
                        toastSafe(ctx, ok ? "已撤回" : "撤回失败（可能不是你的消息）");
                        d.dismiss();
                    }
                }), weight()); }

        if (onlyExtract) {
            row1.addView(iconAction(ctx, R.drawable.ic_a_copy, "提取卡片", 0xFF6B7A8F,
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            d.dismiss();
                            try {
                                Object rec3 = Refl.call(msgItem, "getMsgRecord");
                                if (rec3 == null) rec3 = Refl.field(msgItem, "msgRecord");
                                FeatureMsgExtract.showExtract(rec3 != null ? rec3 : msgItem);
                            } catch (Throwable t) {
                                HookLog.e("提取卡片失败", t);
                            }
                        }
                    }), weight());
        }
        root.addView(row1, matchRow());

        if (!onlyExtract) {
        LinearLayout row2 = new LinearLayout(ctx);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(iconAction(ctx, R.drawable.ic_a_mute, "禁言", 0xFF6B7A8F,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        d.dismiss();
                        chooseMuteDuration(ctx, safePeer, safeUid, nick);
                    }
                }), weight());
        row2.addView(iconAction(ctx, R.drawable.ic_a_kick, "踢出群聊", 0xFFE06666,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        d.dismiss();
                        confirmKickLike(ctx, "踢出", safePeer, safeUid, nick, false);
                    }
                }), weight());
        root.addView(row2, matchRow());
        }

        
        android.widget.FrameLayout wrap = new android.widget.FrameLayout(ctx);
        wrap.setPadding(dp(ctx, 22), 0, dp(ctx, 22), 0);
        wrap.addView(root, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));
        d.setContentView(wrap);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(0));
            d.getWindow().setGravity(Gravity.CENTER);
            d.getWindow().setDimAmount(0.32f);
        }
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    
    
    private boolean modifyCardName(String group, String uid, String card) {
        return GroupManageApi.modifyCard(group, uid, card);
    }

    private boolean muteMember(String group, String uid, int seconds) {
        return GroupManageApi.mute(group, uid, seconds);
    }

    private boolean kick(String group, String uid) {
        return GroupManageApi.kick(group, uid);
    }

    private static boolean isOwnerOrAdmin(String groupUin) {
        Long t = ROLE_TIME.get(groupUin);
        Boolean cached = ROLE_CACHE.get(groupUin);
        long now = System.currentTimeMillis();
        if (cached != null && t != null && now - t < ROLE_TTL_MS) return cached;
        boolean result = GroupManageApi.isOwnerOrAdmin(groupUin);
        ROLE_CACHE.put(groupUin, Boolean.valueOf(result));
        ROLE_TIME.put(groupUin, Long.valueOf(now));
        HookLog.i("群 " + groupUin + " 身份: " + (result ? "群主/管理员" : "普通群员"));
        return result;
    }

    private static final java.util.Map<String, Boolean> ROLE_CACHE =
            java.util.Collections.synchronizedMap(
                    new java.util.HashMap<String, Boolean>());
    private static final java.util.Map<String, Long> ROLE_TIME =
            java.util.Collections.synchronizedMap(
                    new java.util.HashMap<String, Long>());

    private static final long ROLE_TTL_MS = 5 * 60 * 1000L;

    private static Bitmap drawableToBitmap(View avatarView) {
        try {
            int w = avatarView.getWidth();
            int h = avatarView.getHeight();
            if (w <= 0 || h <= 0) return null;
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(b);
            avatarView.draw(c);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap circleBitmap(Bitmap src, int size) {
        try {
            Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            android.graphics.Paint p = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG);
            android.graphics.BitmapShader sh = new android.graphics.BitmapShader(src,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP);
            android.graphics.Matrix m = new android.graphics.Matrix();
            float scale = Math.max((float) size / src.getWidth(),
                    (float) size / src.getHeight());
            m.setScale(scale, scale);
            sh.setLocalMatrix(m);
            p.setShader(sh);
            c.drawCircle(size / 2f, size / 2f, size / 2f, p);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    private static TextView bigText(Context c, String s, float size, int color) {
        TextView t = new TextView(c);
        t.setText(s == null ? "" : s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, dp(c, 2), 0, dp(c, 2));
        return t;
    }

    private static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0xFFF0F2F6);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1)));
        lp.topMargin = dp(c, 16);
        lp.bottomMargin = dp(c, 6);
        v.setLayoutParams(lp);
        return v;
    }

    private static View sectionTitle(Context c, String s) {
        TextView t = bigText(c, s, 13, 0xFF9AA7B8);
        t.setPadding(0, dp(c, 16), 0, dp(c, 12));
        return t;
    }

    private static View iconAction(Context c, int iconRes, String label, int labelColor,
                                   View.OnClickListener l) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(c, 2), dp(c, 13), dp(c, 2), dp(c, 13));
        box.setClickable(true);
        box.setOnClickListener(l);

        android.graphics.drawable.StateListDrawable sd =
                new android.graphics.drawable.StateListDrawable();
        GradientDrawable pressed = new GradientDrawable();
        pressed.setCornerRadius(dp(c, 12));
        pressed.setColor(0x22E85D9C);
        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(dp(c, 12));
        normal.setColor(0x00000000);
        sd.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sd.addState(new int[]{}, normal);
        box.setBackgroundDrawable(sd);

        android.widget.ImageView ic = new android.widget.ImageView(c);
        ic.setImageResource(iconRes);
        box.addView(ic, new LinearLayout.LayoutParams(dp(c, 31), dp(c, 31)));

        TextView lb = new TextView(c);
        lb.setText(label);
        lb.setTextSize(12.5f);
        lb.setTextColor(labelColor);
        lb.setGravity(Gravity.CENTER);
        lb.setPadding(0, dp(c, 8), 0, 0);
        box.addView(lb);

        return box;
    }

    private static LinearLayout.LayoutParams matchRow() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 2;
        return lp;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private static int dp(Context c, int v) {
        
        return com.cathelperawa.owo.app.ui.UiKit.dp(c, v);
    }

    private static void UiApiCopy(Context ctx, String s) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("uid", s));
        } catch (Throwable ignore) {
        }
    }

    

    
    private void confirmKickLike(final Context ctx, final String action,
                                 final String group, final String uid, String nick,
                                 final boolean mute) {
        if (group == null || group.isEmpty() || uid == null || uid.isEmpty()) {
            toastSafe(ctx, "拿不到群号 / UID");
            return;
        }

        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(ctx, 4), 0, dp(ctx, 8));
        TextView tip = bigText(ctx, "此操作不可撤销，需要你是群主或管理员", 12, 0xFFD9534F);
        tip.setPadding(0, dp(ctx, 4), 0, dp(ctx, 4));
        body.addView(tip);

        PanelApi.show(ctx, null, action, nick + " · " + group, body,
                "确认" + action, new PanelApi.OnOk() {
                    @Override public void run() {
                        boolean ok = mute ? muteMember(group, uid, 600) : kick(group, uid);
                        toastSafe(ctx, ok ? ("已提交" + action + "，看群成员列表确认")
                                : ("调用失败（看模块日志）"));
                    }
                });
    }

    
    
    private static android.widget.EditText findInput(View v, int depth) {
        if (v == null || depth > 25) return null;
        if (v instanceof android.widget.EditText && v.isShown() && v.getWidth() > 0) {
            return (android.widget.EditText) v;
        }
        if (!(v instanceof android.view.ViewGroup)) return null;
        android.view.ViewGroup g = (android.view.ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            android.widget.EditText r = findInput(g.getChildAt(i), depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    private static boolean insertAt(Context ctx, String nick) {
        try {
            if (!(ctx instanceof android.app.Activity)) return false;
            View root = ((android.app.Activity) ctx).getWindow().getDecorView();
            android.widget.EditText et = findInput(root, 0);
            if (et == null) return false;
            et.setText("@" + nick + " ");
            et.setSelection(et.getText().length());
            et.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void chooseCardName(final Context ctx, final String group,
                                final String uid, String nick) {
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(ctx, 2), dp(ctx, 6), dp(ctx, 2), 0);

        final android.widget.EditText et = new android.widget.EditText(ctx);
        et.setHint("新的群名片（留空则清空）");
        et.setHintTextColor(0xFFBFC9D6);
        et.setTextSize(15);
        et.setSingleLine(true);
        et.setTextColor(0xFF44546A);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0x66FFFFFF);
        etBg.setCornerRadius(dp(ctx, 14));
        etBg.setStroke(Math.max(1, dp(ctx, 1)), 0x33A9BEDC);
        et.setBackgroundDrawable(etBg);
        et.setPadding(dp(ctx, 14), dp(ctx, 11), dp(ctx, 14), dp(ctx, 11));
        body.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 46)));

        PanelApi.show(ctx, null, "改群名片", nick, body, "保存",
                new PanelApi.OnOk() {
                    @Override public void run() {
                        String card = et.getText() == null ? "" : et.getText().toString();
                        boolean ok = modifyCardName(group, uid, card);
                        toastSafe(ctx, ok ? "已提交修改" : "修改失败（看日志）");
                    }
                });
    }

    private void chooseMuteDuration(final Context ctx, final String group,
                                    final String uid, String nick) {
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(ctx, 2), dp(ctx, 6), dp(ctx, 2), 0);

        final android.widget.EditText et = new android.widget.EditText(ctx);
        et.setHint("10分钟 / 1h / 30秒 / 2天6小时");
        et.setHintTextColor(0xFFBFC9D6);
        et.setTextSize(15);
        et.setSingleLine(true);
        et.setTextColor(0xFF44546A);
        et.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(0x66FFFFFF);
        etBg.setCornerRadius(dp(ctx, 14));
        etBg.setStroke(Math.max(1, dp(ctx, 1)), 0x33A9BEDC);
        et.setBackgroundDrawable(etBg);
        et.setPadding(dp(ctx, 14), dp(ctx, 11), dp(ctx, 14), dp(ctx, 11));

        body.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 46)));

        TextView unban = PanelApi.pillButton(ctx, "解除禁言", false);
        unban.setTextColor(0xFFE06666);
        unban.setTextSize(14);
        unban.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 44));
        ulp.topMargin = dp(ctx, 14);
        unban.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean ok = muteMember(group, uid, 0);
                toastSafe(ctx, ok ? "已解除禁言（看群消息确认）" : "解除失败（看日志）");
            }
        });
        body.addView(unban, ulp);

        PanelApi.show(ctx, null, "禁言 " + nick,
                "填时长，支持 秒 / 分钟 / 小时 / 天", body, "确认禁言",
                new PanelApi.OnOk() {
                    @Override public void run() {
                        String txt = et.getText() == null ? "" : et.getText().toString();
                        int sec = parseDuration(txt);
                        if (sec <= 0) {
                            toastSafe(ctx, "看不懂时长：「" + txt + "」");
                            return;
                        }
                        boolean ok = muteMember(group, uid, sec);
                        toastSafe(ctx, ok ? ("已禁言 " + txt) : "禁言失败（看模块日志）");
                    }
                });
    }

    private static int parseDuration(String s) {
        if (s == null) return 0;
        String t = s.trim().toLowerCase().replace(" ", "").replace("　", "");
        if (t.isEmpty()) return 0;

        java.util.regex.Pattern p =
                java.util.regex.Pattern.compile("(\\d+)\\s*([a-z\\u4e00-\\u9fa5]*)");
        java.util.regex.Matcher m = p.matcher(t);
        int total = 0;
        boolean any = false;
        while (m.find()) {
            long n;
            try {
                n = Long.parseLong(m.group(1));
            } catch (Throwable ignore) {
                continue;
            }
            String u = m.group(2) == null ? "" : m.group(2);
            int mult = 1;
            if (u.contains("秒") || u.startsWith("s")) {
                mult = 1;
            } else if (u.contains("分") || u.startsWith("m")) {
                mult = 60;
            } else if (u.contains("时") || u.startsWith("h")) {
                mult = 3600;
            } else if (u.contains("天") || u.contains("日") || u.startsWith("d")) {
                mult = 86400;
            }
            long add = n * mult;
            if (add > Integer.MAX_VALUE - total) add = Integer.MAX_VALUE - total;
            total += (int) add;
            any = true;
        }
        return any ? total : 0;
    }

    
    private Object groupService() {
        try {
            Object kernel = QQEnv.kernelService();
            if (kernel == null) return null;
            Object impl = Refl.call(kernel, "getGroupService");
            if (impl == null) return null;
            Object nativeSvc = Refl.call(impl, "getService");
            return nativeSvc != null ? nativeSvc : impl;
        } catch (Throwable t) {
            return null;
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return 0;
        }
    }

    private void confirmKick(final Context ctx, final String group, final String uid,
                             String nick) {
        if (group == null || group.isEmpty() || uid == null || uid.isEmpty()) {
            toastSafe(ctx, "拿不到群号 / UID，踢不了");
            return;
        }
        new AlertDialog.Builder(ctx)
                .setTitle("踢出群聊")
                .setMessage("把「" + nick + "」踢出群 " + group + "？\n\n"
                        + "★ 这是不可逆操作，且需要你是群主 / 管理员。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认踢出", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        boolean ok = kick(group, uid);
                        toastSafe(ctx, ok ? "已提交，看群成员列表确认" : "调用失败（看模块日志）");
                    }
                })
                .show();
    }

    
    private static Object makeCallback(final Context ctx, ClassLoader cl, String ifaceName) {
        try {
            Class<?> iface = HookApi.findClass(cl, ifaceName);
            if (iface == null) return null;
            return java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[]{iface},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            try {
                                if (args != null && args.length > 0) {
                                    Object r = args[0];
                                    toastSafe(ctx, "踢人回调: code="
                                            + Refl.fieldInt(r, "resultCode",
                                                    Refl.fieldInt(r, "errorCode", -1)));
                                }
                            } catch (Throwable ignore) {
                            }
                            return null;
                        }
                    });
        } catch (Throwable t) {
            return null;
        }
    }

    private static void toastSafe(Context ctx, String msg) {
        try {
            com.cathelperawa.owo.app.bridge.CatToast.show(msg);
        } catch (Throwable t) {
            try {
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show();
            } catch (Throwable ignore) {
            }
        }
    }
}
