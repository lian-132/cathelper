package com.cathelperawa.owo.app.ai;

import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class TtsEngine {

    private TtsEngine() {}

    private static final String FREE_URL = "https://fanyi.baidu.com/gettts?lan=zh&text=";

    public static boolean synth(String text, File outMp3) {
        return synthAs(text, null, outMp3);
    }

    public static boolean synthAs(String text, String voice, File outMp3) {
        if (text == null || text.trim().length() == 0) return false;
        String body = text.trim();
        if (body.length() > 500) body = body.substring(0, 500);
        try {
            if (AiConfig.ttsUseApi()) {
                if (synthByApi(body, voice, outMp3)) return true;

                com.cathelperawa.owo.app.MsgStore.add("[TTS] 自建接口失败，回落免费通道");
            }
            return synthFree(body, outMp3);
        } catch (Throwable t) {
            HookLog.e("TTS 合成异常", t);
            return false;
        }
    }

    private static boolean synthByApi(String text, String voiceOverride, File out) {
        HttpURLConnection conn = null;
        try {
            String base = AiConfig.ttsBaseUrl();
            while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            if (!base.endsWith("/audio/speech")) base = base + "/audio/speech";

            JSONObject body = new JSONObject();
            body.put("model", AiConfig.ttsModel());
            body.put("input", text);
            body.put("voice", (voiceOverride == null || voiceOverride.trim().length() == 0)
                    ? AiConfig.ttsVoice() : voiceOverride.trim());
            body.put("response_format", "mp3");

            URL u = new URL(base);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + AiConfig.ttsKey());

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String err = readText(conn.getErrorStream());
                com.cathelperawa.owo.app.MsgStore.add("[TTS] 接口返回 " + code + " "
                        + (err == null ? "" : err.substring(0, Math.min(120, err.length()))));
                return false;
            }
            InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
                total += n;
            }
            fos.close();
            is.close();
            if (total < 500) {
                com.cathelperawa.owo.app.MsgStore.add("[TTS] 返回数据太小（" + total + " 字节），当失败");
                return false;
            }
            com.cathelperawa.owo.app.MsgStore.add("[TTS] 自建接口合成成功 " + total + " 字节");
            return true;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.MsgStore.add("[TTS] 自建接口异常 " + t);
            return false;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignore) {}
        }
    }

    private static boolean synthFree(String text, File out) {
        HttpURLConnection conn = null;
        try {
            String url = FREE_URL + URLEncoder.encode(text, "UTF-8") + "&spd=5&source=web";
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int code = conn.getResponseCode();
            if (code != 200) {
                com.cathelperawa.owo.app.MsgStore.add("[TTS] 免费通道 HTTP " + code);
                return false;
            }
            InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
                total += n;
            }
            fos.close();
            is.close();
            com.cathelperawa.owo.app.MsgStore.add("[TTS] 免费通道合成 " + total + " 字节");
            return total > 500;
        } catch (Throwable t) {
            com.cathelperawa.owo.app.MsgStore.add("[TTS] 免费通道异常 " + t);
            return false;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignore) {}
        }
    }

    private static String readText(InputStream is) {
        if (is == null) return null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    private static final String[] BUILTIN = {
            "alloy", "echo", "fable", "onyx", "nova", "shimmer",
            "zh-CN-XiaoxiaoNeural", "zh-CN-YunxiNeural", "zh-CN-YunyangNeural", "zh-CN-XiaoyiNeural"
    };

    public static String[] fetchVoices() {
        if (!AiConfig.ttsUseApi()) {
            return new String[]{String.join(",", BUILTIN), "内置候选（没填密钥，只能用免费通道）"};
        }
        String base = AiConfig.ttsBaseUrl();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String[] paths = {"/audio/voices", "/voices", "/audio/voice/list"};
        for (int i = 0; i < paths.length; i++) {
            String got = tryList(base + paths[i]);
            if (got != null && got.length() > 0) {
                return new String[]{got, "接口 " + paths[i]};
            }
        }
        return new String[]{String.join(",", BUILTIN), "内置候选（该接口不支持列表）"};
    }

    private static String tryList(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Authorization", "Bearer " + AiConfig.ttsKey());
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) return null;
            String body = readText(conn.getInputStream());
            if (body == null || body.length() == 0) return null;

            java.util.List<String> out = new java.util.ArrayList<String>();
            if (body.trim().startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(body.trim());
                for (int i = 0; i < arr.length(); i++) {
                    Object o = arr.opt(i);
                    if (o instanceof String) out.add((String) o);
                    else if (o instanceof JSONObject) {
                        JSONObject jo = (JSONObject) o;
                        String id = jo.optString("id", jo.optString("name", ""));
                        if (id.length() > 0) out.add(id);
                    }
                }
            } else {
                JSONObject jo = new JSONObject(body);
                org.json.JSONArray arr = jo.optJSONArray("data");
                if (arr == null) arr = jo.optJSONArray("voices");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object o = arr.opt(i);
                        if (o instanceof String) out.add((String) o);
                        else if (o instanceof JSONObject) {
                            String id = ((JSONObject) o).optString("id",
                                    ((JSONObject) o).optString("name", ""));
                            if (id.length() > 0) out.add(id);
                        }
                    }
                }
            }
            return out.isEmpty() ? null : String.join(",", out);
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignore) {}
        }
    }

    public static String channelName() {
        return AiConfig.ttsUseApi() ? "自建接口 · " + AiConfig.ttsVoice() : "内置免费 TTS";
    }
}
