package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.feature.FeatureChatMix;

public final class ChatMixApi {

    private ChatMixApi() {}

    

    
    public static boolean isOn() {
        return FeatureChatMix.on();
    }

    public static boolean isAlwaysMode() {
        return FeatureChatMix.always();
    }

    public static void setAlwaysMode(boolean on) {
        FeatureChatMix.setAlways(on);
    }

    public static void resetAlwaysMode() {
        FeatureChatMix.resetAlways();
    }

    

    
    public static boolean hasInput() {
        return FeatureChatMix.hasInput();
    }

    public static boolean insertPic(String path) {
        if (path == null || path.length() == 0) return false;
        return FeatureChatMix.insertPic(path, isGif(path));
    }

    public static boolean insertPic(String path, boolean isGif) {
        if (path == null || path.length() == 0) return false;
        return FeatureChatMix.insertPic(path, isGif);
    }

    public static boolean insertInput(String text) {
        if (text == null || text.length() == 0) return false;
        return FeatureChatMix.appendToInput(text);
    }

    public static String inputText() {
        return FeatureChatMix.inputText();
    }

    public static int inputPicCount() {
        return FeatureChatMix.inputPicCount();
    }

    public static boolean clearInput() {
        return FeatureChatMix.clearInput();
    }

    

    
    public static boolean openFullScreen() {
        return FeatureChatMix.openFullScreen();
    }

    public static boolean isFullScreenReady() {
        return FeatureChatMix.hasAioContext();
    }

    

    
    public static boolean isGif(String path) {
        if (path == null) return false;
        String low = path.toLowerCase();
        return low.endsWith(".gif") || low.endsWith(".webp");
    }
}
