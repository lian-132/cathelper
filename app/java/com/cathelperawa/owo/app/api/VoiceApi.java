package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.QQEnv;
import com.cathelperawa.owo.app.bridge.VoiceSilk;
import com.cathelperawa.owo.app.bridge.VoiceUpload;

public final class VoiceApi {

    private VoiceApi() {}

    public static String tts2Silk(String text, String outPath) {
        return VoiceSilk.text2Silk(QQEnv.appContext(), text, outPath);
    }

    public static String upload(String slk, String json) {
        return VoiceUpload.upload(QQEnv.appContext(), slk, json, 15000L);
    }

    public static boolean uploadOk(String slk, String json) {
        return VoiceUpload.uploadOk(QQEnv.appContext(), slk, json, 15000L);
    }

    public static String send(String text, String json) {
        String slk = tts2Silk(text, IoApi.dataDir() + "/voice_" + UtilApi.now() + ".slk");
        if (slk == null) return null;
        return upload(slk, json);
    }
}
