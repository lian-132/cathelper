package com.cathelperawa.owo.app.ai;

import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cathelperawa.owo.app.ui.Page;

public final class AiTtsPage extends Page {

    private static final int C_MAIN = 0xFF3C4B60;
    private static final int C_SUB = 0xFF8A9BB0;

    private LinearLayout body;
    private LinearLayout content;

    public AiTtsPage(Host host) {
        super(host);
    }

    @Override
    public View build() {
        View[] out = new View[2];
        body = pageRoot("语音合成", "填了密钥就用你自己的接口，可选音色", out, true, 13, "bg_ai", 0x66);
        content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, matchWrap(0));
        render();
        return out[0];
    }

    private void render() {
        content.removeAllViews();

        LinearLayout head = whiteCard(dp(20), 0xF2FFFFFF);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(18), dp(16), dp(18), dp(16));
        boolean useApi = AiConfig.ttsUseApi();
        head.addView(text(useApi ? "正在用：你的接口" : "正在用：内置免费通道",
                17, useApi ? 0xFF2E9E5B : C_MAIN, true, dp(4)), matchWrap(0));
        head.addView(text(useApi
                        ? AiConfig.ttsModel() + " · 音色 " + AiConfig.ttsVoice()
                        : "单音色、不用注册。填下面的密钥就能换成自己的（可选音色）",
                11.5f, C_SUB, false, 0), matchWrap(0));
        content.addView(head, matchWrap(dp(14)));

        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(10), dp(16), dp(16));

        card.addView(field("接口地址", "ai_tts_url", AiConfig.ttsBaseUrl(),
                "https://api.openai.com/v1"), matchWrap(dp(6)));
        card.addView(field("密钥", "ai_tts_key", AiConfig.ttsKey(),
                "sk-...  （留空＝用内置免费通道）"), matchWrap(dp(6)));
        card.addView(field("模型", "ai_tts_model", AiConfig.ttsModel(),
                "tts-1"), matchWrap(dp(6)));
        card.addView(field("音色", "ai_tts_voice", AiConfig.ttsVoice(),
                "alloy / nova / shimmer / 各家自己的名字"), matchWrap(dp(6)));

        TextView note = text("· 接口要兼容 POST {地址}/audio/speech（OpenAI TTS、各种中转都行）\n"
                        + "· 接口失败会自动回落到免费通道，不会没声音\n"
                        + "· 改完立刻生效，不用重启",
                10.5f, C_SUB, false, 0);
        note.setPadding(0, dp(10), 0, 0);
        card.addView(note, matchWrap(0));
        content.addView(card, matchWrap(dp(14)));

        content.addView(voiceCard(), matchWrap(dp(14)));
    }

    private View voiceCard() {
        LinearLayout card = whiteCard(dp(20), 0xF2FFFFFF);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(10), dp(16), dp(16));

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("音色", 14, C_MAIN, true, 0);
        head.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView fetch = text("拉取列表", 12, 0xFFE85D9C, true, 0);
        fetch.setPadding(dp(10), dp(6), dp(10), dp(6));
        head.addView(fetch, wrapWrap());
        card.addView(head, matchWrap(dp(4)));

        final TextView hint = text("点音色即切换并试听（统一念「你好呀」）",
                10.5f, C_SUB, false, 0);
        card.addView(hint, matchWrap(dp(4)));

        final LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        card.addView(box, matchWrap(dp(4)));

        renderVoices(box, hint);
        press(fetch);
        fetch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hint.setText("正在拉取音色列表…");
                new Thread(new Runnable() {
                    @Override public void run() {
                        final String[] r = TtsEngine.fetchVoices();
                        runOnUi(new Runnable() {
                            @Override public void run() {
                                VOICES = r[0];
                                VOICE_SRC = r[1];
                                hint.setText("来源：" + r[1]);
                                renderVoices(box, hint);
                            }
                        });
                    }
                }, "cat-tts-voices").start();
            }
        });
        return card;
    }

    private static volatile String VOICES = null;
    private static volatile String VOICE_SRC = "";

    private void renderVoices(LinearLayout box, TextView hint) {
        box.removeAllViews();
        String raw = VOICES;
        if (raw == null) {

            raw = "alloy,echo,fable,onyx,nova,shimmer,zh-CN-XiaoxiaoNeural,zh-CN-YunxiNeural";
            if (VOICE_SRC.length() == 0) VOICE_SRC = "内置候选（点「拉取列表」试试你的接口）";
        }
        String[] arr = raw.split(",");
        String cur = AiConfig.ttsVoice();
        for (int i = 0; i < arr.length && i < 40; i++) {
            final String v = arr[i].trim();
            if (v.length() == 0) continue;
            box.addView(voiceRow(v, v.equals(cur), hint), matchWrap(dp(2)));
        }
    }

    private View voiceRow(final String voice, boolean selected, final TextView hint) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(11), dp(10), dp(11));

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(text(voice, 13.5f, selected ? 0xFF2E9E5B : C_MAIN, selected, 0), matchWrap(0));
        if (selected) {
            txt.addView(text("当前使用", 10, 0xFF2E9E5B, false, 0), matchWrap(0));
        }
        row.addView(txt, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView play = text("▶ 试听", 12, 0xFF3C78D8, true, 0);
        play.setPadding(dp(12), dp(6), dp(4), dp(6));
        row.addView(play, wrapWrap());

        final View.OnClickListener act = new View.OnClickListener() {
            @Override public void onClick(View v) {
                AiConfig.setTtsVoice(voice);
                hint.setText("已切到「" + voice + "」，正在合成试听…");
                preview(voice, hint);
            }
        };
        row.setOnClickListener(act);
        play.setOnClickListener(act);
        return row;
    }

    private void preview(final String voice, final TextView hint) {
        new Thread(new Runnable() {
            @Override public void run() {
                final java.io.File mp3 = new java.io.File(ctx.getCacheDir(),
                        "tts_preview_" + System.currentTimeMillis() + ".mp3");
                final boolean ok = TtsEngine.synthAs("你好呀", voice, mp3);
                runOnUi(new Runnable() {
                    @Override public void run() {
                        if (!ok) {
                            hint.setText("试听失败：接口没返回音频（检查密钥/地址/模型）");
                            return;
                        }
                        hint.setText("正在播放「" + voice + "」");
                        playFile(mp3, hint);
                        render();
                    }
                });
            }
        }, "cat-tts-preview").start();
    }

    private android.media.MediaPlayer mp;

    private void playFile(java.io.File f, final TextView hint) {
        try {
            stopPlayer();
            mp = new android.media.MediaPlayer();
            mp.setDataSource(f.getAbsolutePath());
            mp.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(android.media.MediaPlayer mediaPlayer) {
                    stopPlayer();
                    try { f.delete(); } catch (Throwable ignore) {}
                    hint.setText("试听结束，点别的音色继续比");
                }
            });
            mp.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override public boolean onError(android.media.MediaPlayer mediaPlayer, int i, int j) {
                    stopPlayer();
                    hint.setText("播放失败（文件可能不是 mp3）");
                    return true;
                }
            });
            mp.prepare();
            mp.start();
        } catch (Throwable t) {
            stopPlayer();
            hint.setText("播放异常 " + t);
        }
    }

    private void stopPlayer() {
        try {
            if (mp != null) {
                mp.release();
                mp = null;
            }
        } catch (Throwable ignore) {}
    }

    private void runOnUi(Runnable r) {
        try {
            android.app.Activity a = activity();
            if (a != null) a.runOnUiThread(r);
            else r.run();
        } catch (Throwable t) {
            r.run();
        }
    }

    private View field(String label, final String key, String cur, String hint) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(label, 12, C_MAIN, true, dp(3)), matchWrap(0));

        EditText et = new EditText(ctx);
        et.setText(cur == null ? "" : cur);
        et.setHint(hint);
        et.setTextSize(13);
        et.setTextColor(C_MAIN);
        et.setSingleLine(true);
        et.setHintTextColor(0xFFB6C1D0);
        et.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x66F0F4F8);
        bg.setCornerRadius(dp(10));
        et.setBackground(bg);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                AiConfig.setString(key, e.toString());
            }
        });
        box.addView(et, matchWrap(0));
        return box;
    }
}
