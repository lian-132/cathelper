package com.cathelperawa.owo.app.ui;

import android.R;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.feature.ArkSigner;

public final class GameCardSheet {
    private GameCardSheet() {
    }

    public static void show(Activity activity) {
        if (activity == null) {
            return;
        }
        float f = activity.getResources().getDisplayMetrics().density;
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-1288684979);
        int i = (int) (16.0f * f);
        linearLayout.setPadding(i, (int) (56.0f * f), i, (int) (24.0f * f));
        LinearLayout linearLayout2 = new LinearLayout(activity);
        linearLayout2.setOrientation(1);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-722689);
        gradientDrawable.setCornerRadius(22.0f * f);
        linearLayout2.setBackgroundDrawable(gradientDrawable);
        linearLayout2.setPadding(i, i, i, i);
        TextView textView = new TextView(activity);
        textView.setText("直签 · getShareInfo");
        textView.setTextSize(17.0f);
        textView.setTextColor(-13681066);
        textView.setPadding(0, 0, 0, (int) (4.0f * f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(activity);
        textView2.setText("只走 QQ 内置签名，参数随便调");
        textView2.setTextSize(11.0f);
        textView2.setTextColor(-7693131);
        int i2 = (int) (10.0f * f);
        textView2.setPadding(0, 0, 0, i2);
        linearLayout2.addView(textView2);
        EditText field = field(activity, f, "卡片标题", "鲸鲸测试卡");
        EditText field2 = field(activity, f, "封面图直链", "https://picsum.photos/seed/game1/600/338");
        EditText field3 = field(activity, f, "jumpUrl", "");
        EditText field4 = field(activity, f, "appid", "1105198412");
        EditText field5 = field(activity, f, "appidRich（可空）", "");
        EditText field6 = field(activity, f, "templetType", "3");
        EditText field7 = field(activity, f, "scene", "8");
        linearLayout2.addView(field);
        linearLayout2.addView(field2);
        linearLayout2.addView(field3);
        linearLayout2.addView(field4);
        linearLayout2.addView(field5);
        linearLayout2.addView(field6);
        linearLayout2.addView(field7);
        LinearLayout linearLayout3 = new LinearLayout(activity);
        linearLayout3.setOrientation(0);
        linearLayout3.setPadding(0, (int) (12.0f * f), 0, 0);
        TextView btn = btn(activity, f, "关闭", -1051654, -10851451);
        btn.setOnClickListener(new View.OnClickListener() { 
            @Override 
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        int i3 = (int) (44.0f * f);
        linearLayout3.addView(btn, new LinearLayout.LayoutParams(0, i3, 1.0f));
        TextView btnProbe = btn(activity, f, "探测通道", -1, -13681066);
        LinearLayout.LayoutParams lpProbe = new LinearLayout.LayoutParams(0, i3, 1.0f);
        lpProbe.leftMargin = i2;
        linearLayout3.addView(btnProbe, lpProbe);
        btnProbe.setOnClickListener(new View.OnClickListener() { 
            @Override 
            public void onClick(View view) {
                ArkSigner.probeEgameSign("", "https://picsum.photos/seed/game1/600/338", "鲸鲸测试卡", new ArkSigner.SendCb() {
                    @Override 
                    public void done(boolean z, final String str) {
                        activity.runOnUiThread(new Runnable() {
                            @Override 
                            public void run() {
                                CatToast.show("探测完成，结果见 logcat CATHELPER");
                                try {
                                    java.io.File f = new java.io.File(activity.getFilesDir(), "gc_probe.txt");
                                    java.io.FileWriter fw = new java.io.FileWriter(f, true);
                                    fw.write(str);
                                    fw.write("\n");
                                    fw.close();
                                    CatToast.show("已写入 " + f.getAbsolutePath());
                                } catch (Throwable th) {
                                    CatToast.show("写文件失败: " + th);
                                }
                            }
                        });
                    }
                });
                CatToast.show("探测中…");
            }
        });
        TextView btn2 = btn(activity, f, "签名并发送", -1020763, -1);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, i3, 1.4f);
        layoutParams.leftMargin = i2;
        linearLayout3.addView(btn2, layoutParams);
        linearLayout2.addView(linearLayout3);
        btn2.setOnClickListener(new AnonymousClass2(field, field2, field3, field4, field5, field6, field7, btn2, activity, dialog));
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(linearLayout2);
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        dialog.setContentView(linearLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout(-1, -1);
        }
        dialog.show();
    }

    static class AnonymousClass2 implements View.OnClickListener {
        final  Activity val$act;
        final  Dialog val$dlg;
        final  EditText val$eAppid;
        final  EditText val$eJump;
        final  EditText val$ePic;
        final  EditText val$eRich;
        final  EditText val$eScene;
        final  EditText val$eTitle;
        final  EditText val$eTpl;
        final  TextView val$send;

        AnonymousClass2(EditText editText, EditText editText2, EditText editText3, EditText editText4, EditText editText5, EditText editText6, EditText editText7, TextView textView, Activity activity, Dialog dialog) {
            this.val$eTitle = editText;
            this.val$ePic = editText2;
            this.val$eJump = editText3;
            this.val$eAppid = editText4;
            this.val$eRich = editText5;
            this.val$eTpl = editText6;
            this.val$eScene = editText7;
            this.val$send = textView;
            this.val$act = activity;
            this.val$dlg = dialog;
        }

        @Override 
        public void onClick(View view) {
            String trim = this.val$eTitle.getText().toString().trim();
            String trim2 = this.val$ePic.getText().toString().trim();
            String trim3 = this.val$eJump.getText().toString().trim();
            String trim4 = this.val$eAppid.getText().toString().trim();
            String trim5 = this.val$eRich.getText().toString().trim();
            int parseInt = GameCardSheet.parseInt(this.val$eTpl.getText().toString(), 3);
            int parseInt2 = GameCardSheet.parseInt(this.val$eScene.getText().toString(), 8);
            this.val$send.setText("签名中…");
            this.val$send.setEnabled(false);
            
            final String fTitle = trim.length() == 0 ? "视频" : trim;
            final String fPic = trim2;
            final String fJump = trim3.length() == 0 ? "http%3A%2F%2Fimgcache.qq.com" : trim3;
            final String fVid = trim5.length() == 0 ? trim4 : trim5;
            final String metaJson = "{\"news\":{\"title\":\"" + fTitle + "\",\"desc\":\"视频分享\",\"preview\":\"" + fPic + "\",\"jumpUrl\":\"" + fJump + "\",\"videoUrl\":\"" + fVid + "\",\"type\":\"video\",\"buttons\":[{\"text\":\"TA的战绩\",\"url\":\"" + fJump + "\"},{\"text\":\"点我\",\"url\":\"" + fJump + "\"}]}}";
            new Thread(new Runnable() { 
                @Override 
                public void run() {
                    ArkSigner.Result r = ArkSigner.signCustomArkBiz("com.tencent.tuwen.lua", "guild.share", fTitle, metaJson, null);
                    final boolean ok = r != null && r.json != null;
                    final String err = r == null ? "无返回" : (r.error == null ? "" : r.error);
                    final String json = ok ? r.json : null;
                    AnonymousClass2.this.val$act.runOnUiThread(new Runnable() { 
                        @Override 
                        public void run() {
                            AnonymousClass2.this.val$send.setText("签名并发送");
                            AnonymousClass2.this.val$send.setEnabled(true);
                            if (ok && json != null) {
                                SendPanel.sendArkJson(AnonymousClass2.this.val$act, json);
                                AnonymousClass2.this.val$dlg.dismiss();
                            } else {
                                CatToast.show("签名失败：" + err);
                            }
                        }
                    });
                }
            }, "videocard-sign2").start();
            if (false) ArkSigner.sendGameCard(trim, "", trim2, trim3, trim4, trim5, parseInt, parseInt2, new ArkSigner.SendCb() { 
                @Override 
                public void done(final boolean z, final String str) {
                    AnonymousClass2.this.val$act.runOnUiThread(new Runnable() { 
                        @Override 
                        public void run() {
                            AnonymousClass2.this.val$send.setText("签名并发送");
                            AnonymousClass2.this.val$send.setEnabled(true);
                            if (z) {
                                CatToast.show("已发送");
                                AnonymousClass2.this.val$dlg.dismiss();
                            } else {
                                CatToast.show("失败：" + str);
                            }
                        }
                    });
                }
            });
        }
    }

    public static int parseInt(String str, int i) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Throwable th) {
            return i;
        }
    }

    private static EditText field(Activity activity, float f, String str, String str2) {
        EditText editText = new EditText(activity);
        editText.setHint(str);
        editText.setText(str2);
        editText.setTextSize(13.0f);
        editText.setSingleLine(true);
        editText.setTextColor(-12824981);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-1);
        gradientDrawable.setCornerRadius(12.0f * f);
        editText.setBackgroundDrawable(gradientDrawable);
        int i = (int) (10.0f * f);
        int i2 = (int) (f * 8.0f);
        editText.setPadding(i, i2, i, i2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = i2;
        editText.setLayoutParams(layoutParams);
        return editText;
    }

    private static TextView btn(Activity activity, float f, String str, int i, int i2) {
        TextView textView = new TextView(activity);
        textView.setText(str);
        textView.setTextSize(14.0f);
        textView.setTextColor(i2);
        textView.setGravity(17);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(f * 14.0f);
        textView.setBackgroundDrawable(gradientDrawable);
        return textView;
    }
}
