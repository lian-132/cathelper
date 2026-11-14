package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.AppConfig;
import com.cathelperawa.owo.app.CatProcessor;

public final class CatifyApi {

    private CatifyApi() {}

    public static String process(String input) {
        if (input == null || input.isEmpty()) return input;
        if (AppConfig.current == null || !AppConfig.current.total) return input;
        if (input.contains("喵")) return input;
        return CatProcessor.process(input, AppConfig.current);
    }

    public static boolean enabled() {
        return AppConfig.current != null && AppConfig.current.total;
    }
}
