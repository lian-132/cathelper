package com.cathelperawa.owo.app;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.List;

public class QQAccessibilityService extends AccessibilityService {

    private static final String TAG = "CatHelperA11y";
    private static final int MAX_EDIT_LEN = 500;

    private volatile boolean processing = false;
    private String lastSet = "";

    private volatile long lastTextChangeTime = 0;
    private volatile boolean lastChangeWasInput = false;
    private String lastTextSnapshot = "";

    private static final List<String> QQ_INPUT_IDS = Arrays.asList(
            "com.tencent.mobileqq:id/inputBar",
            "com.tencent.mobileqq:id/chat_input",
            "com.tencent.mobileqq:id/message_edit",
            "com.tencent.mobileqq:id/sendMessageEditText",
            "com.tencent.mobileqq:id/msg_input_et",
            "com.tencent.mobileqq:id/footer_et_msg",
            "com.tencent.mobileqq:id/input_edit",
            "com.tencent.mobileqq:id/input",
            "com.tencent.mobileqq:id/edtInput",
            "com.tencent.mobileqq:id/et_input",
            "com.tencent.mobileqq:id/inputEdt",
            "com.tencent.mobileqq:id/inputbar",
            "com.tencent.tim:id/inputBar",
            "com.tencent.tim:id/chat_input",
            "com.tencent.tim:id/message_edit",
            "com.tencent.tim:id/sendMessageEditText",
            "com.tencent.tim:id/msg_input_et",
            "com.tencent.tim:id/footer_et_msg",
            "com.tencent.tim:id/input_edit",
            "com.tencent.tim:id/input",
            "com.tencent.tim:id/edtInput",
            "com.tencent.tim:id/et_input",
            "com.tencent.tim:id/inputEdt",
            "com.tencent.tim:id/inputbar"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();
        if (!isTargetPackage(pkg)) return;

        AppConfig cfg = AppConfig.current;
        if (cfg == null || !cfg.total) return;

        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastSet = "";
            return;
        }

        if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            trackTextChange(event, true);
            return;
        }
        if (type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            trackTextChange(event, false);
            return;
        }

        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (isSendButton(event)) {
                AppConfig c = AppConfig.current;
                if (c != null && c.triggerOnPaste) doPasteReplace(c);
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AppConfig.load(this);
        FloatWindowService.a11yService = this;
        Log.d(TAG, "onServiceConnected, total=" + (AppConfig.current != null ? AppConfig.current.total : "null"));
    }

    @Override
    public void onDestroy() {
        FloatWindowService.a11yService = null;
        super.onDestroy();
    }

    public void manualConvert(AppConfig cfg) {
        doManualReplace(cfg);
    }

    private boolean isTargetPackage(String pkg) {
        return pkg.startsWith("com.tencent.mobileqq")
                || pkg.equals("com.tencent.qq")
                || pkg.equals("com.tencent.tim");
    }

    private boolean isSendButton(AccessibilityEvent event) {
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) return false;
        try {
            String t = textOf(node.getText());
            String d = textOf(node.getContentDescription());
            String id = textOf(node.getViewIdResourceName()).toLowerCase();
            String cls = textOf(node.getClassName());
            String label = t + d;
            return label.contains("发送")
                    || label.toLowerCase().contains("send")
                    || (cls.toLowerCase().contains("button") && d.contains("发送"))
                    || id.contains("send");
        } finally {
            node.recycle();
        }
    }

    private void doReplace(AppConfig cfg) {
        if (processing) return;
        processing = true;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            try {
                AccessibilityNodeInfo input = findInputField(root);
                if (input == null) {
                    Log.d(TAG, "send clicked but no input field found");
                    return;
                }
                try {
                    String raw = textOf(input.getText());
                    if (TextUtils.isEmpty(raw)) return;
                    if (raw.length() > MAX_EDIT_LEN) return;
                    if (raw.equals(lastSet)) return;

                    String out = CatProcessor.process(raw, cfg);

                    try {
                        if (com.cathelperawa.owo.app.script.PatchPoint.has("host.typing.text")) {
                            com.cathelperawa.owo.app.script.PatchPoint.PatchCtx pc =
                                    new com.cathelperawa.owo.app.script.PatchPoint.PatchCtx(
                                            "host.typing.text", out, "");
                            com.cathelperawa.owo.app.script.PatchPoint.emit(pc);
                            if (pc.cancelled()) return;
                            out = pc.text();
                        }
                    } catch (Throwable ignore) {
                    }

                    if (TextUtils.equals(out, raw)) return;

                    if (setText(input, out)) {
                        lastSet = out;
                        setCursor(input, out.length());
                        Log.d(TAG, "send-replace: [" + raw + "] -> [" + out + "]");
                    }
                } finally {
                    input.recycle();
                }
            } finally {
                root.recycle();
            }
        } catch (Throwable e) {
            Log.e(TAG, "doReplace error", e);
        } finally {
            processing = false;
        }
    }

    private void trackTextChange(AccessibilityEvent event, boolean fromTextEvent) {
        try {
            String current = readInputText();
            if (current == null) return;

            int diff = current.length() - lastTextSnapshot.length();

            if (fromTextEvent) {
                if (diff > 1) {
                    lastChangeWasInput = false;
                } else if (diff == 1 || diff == 0) {
                    lastChangeWasInput = true;
                }

                lastTextChangeTime = System.currentTimeMillis();
            }
            lastTextSnapshot = current;
        } catch (Throwable ignored) {}
    }

    private String readInputText() {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo input = null;
        try {
            root = getRootInActiveWindow();
            if (root == null) return null;
            input = findInputField(root);
            if (input == null) return null;
            return textOf(input.getText());
        } catch (Throwable e) {
            return null;
        } finally {
            if (input != null) try { input.recycle(); } catch (Throwable ignored) {}
            if (root != null) try { root.recycle(); } catch (Throwable ignored) {}
        }
    }

    private void doManualReplace(AppConfig cfg) {
        if (processing) return;
        processing = true;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            try {
                AccessibilityNodeInfo input = findInputField(root);
                if (input == null) {
                    Log.d(TAG, "float click: 未找到输入框");
                    return;
                }
                try {
                    String raw = textOf(input.getText());
                    if (TextUtils.isEmpty(raw) || raw.length() > MAX_EDIT_LEN) return;
                    if (raw.equals(lastSet)) return;
                    String out = CatProcessor.process(raw, cfg);
                    if (TextUtils.equals(out, raw)) return;
                    if (setText(input, out)) {
                        lastSet = out;
                        setCursor(input, out.length());
                        lastTextSnapshot = out;
                        Log.d(TAG, "float-replace: [" + raw + "] -> [" + out + "]");
                    }
                } finally {
                    input.recycle();
                }
            } finally {
                root.recycle();
            }
        } catch (Throwable e) {
            Log.e(TAG, "doManualReplace error", e);
        } finally {
            processing = false;
        }
    }

    private void doPasteReplace(AppConfig cfg) {
        if (processing) return;
        processing = true;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            try {
                AccessibilityNodeInfo input = findInputField(root);
                if (input == null) return;
                try {
                    String raw = textOf(input.getText());
                    if (TextUtils.isEmpty(raw) || raw.length() > MAX_EDIT_LEN) return;
                    if (raw.equals(lastSet)) return;
                    String out = CatProcessor.process(raw, cfg);
                    if (TextUtils.equals(out, raw)) return;
                    if (setText(input, out)) {
                        lastSet = out;
                        setCursor(input, out.length());
                        Log.d(TAG, "paste-replace: [" + raw + "] -> [" + out + "]");
                    }
                } finally {
                    input.recycle();
                }
            } finally {
                root.recycle();
            }
        } catch (Throwable e) {
            Log.e(TAG, "doPasteReplace error", e);
        } finally {
            processing = false;
        }
    }

    private AccessibilityNodeInfo findInputField(AccessibilityNodeInfo root) {
        for (String id : QQ_INPUT_IDS) {
            try {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
                if (nodes != null && !nodes.isEmpty()) {
                    for (AccessibilityNodeInfo n : nodes) {
                        try {
                            if (n != null && n.isEditable()) return AccessibilityNodeInfo.obtain(n);
                        } finally {
                            if (n != null) n.recycle();
                        }
                    }
                }
            } catch (Throwable ignore) {}
        }

        AccessibilityNodeInfo editable = findEditable(root);
        if (editable != null) return editable;

        AccessibilityNodeInfo byClass = findEditTextByClass(root);
        if (byClass != null) return byClass;

        try {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus != null) return AccessibilityNodeInfo.obtain(focus);
        } catch (Throwable ignore) {}

        return null;
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        try {
            if (node.isEditable()) return AccessibilityNodeInfo.obtain(node);
        } catch (Throwable ignore) {}
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = null;
            try {
                c = node.getChild(i);
                if (c == null) continue;
                AccessibilityNodeInfo got = findEditable(c);
                if (got != null) return got;
            } catch (Throwable ignore) {
            } finally {
                if (c != null) c.recycle();
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findEditTextByClass(AccessibilityNodeInfo node) {
        try {
            CharSequence cs = node.getClassName();
            String cls = cs == null ? "" : cs.toString();
            if (cls.endsWith("EditText")) return AccessibilityNodeInfo.obtain(node);
        } catch (Throwable ignore) {}

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = null;
            try {
                c = node.getChild(i);
                if (c == null) continue;
                AccessibilityNodeInfo got = findEditTextByClass(c);
                if (got != null) return got;
            } catch (Throwable ignore) {
            } finally {
                if (c != null) c.recycle();
            }
        }
        return null;
    }

    private boolean setText(AccessibilityNodeInfo node, String text) {
        try {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        } catch (Throwable e) {
            return false;
        }
    }

    private void setCursor(AccessibilityNodeInfo node, int index) {
        try {
            Bundle sel = new Bundle();
            sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, index);
            sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, index);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel);
        } catch (Throwable ignore) {}
    }

    private String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
