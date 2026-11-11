package com.cathelperawa.owo.app.feature;

import android.content.Context;

import com.cathelperawa.owo.app.bridge.CatToast;
import com.cathelperawa.owo.app.bridge.CurrentChat;
import com.cathelperawa.owo.app.core.Bg;
import com.cathelperawa.owo.app.core.HookLog;

import org.json.JSONObject;

public final class ArkSmart {

    public static final int CH_RAW = 0;
    
    public static final int CH_SSO = 1;
    
    public static final int CH_MINI = 2;
    
    public static final int CH_CARD77 = 3;

    public static final class Kind {
        public final String name;      
        public final String channel;   
        public final int ch;
        public final String refer;

        Kind(String name, String channel, int ch, String refer) {
            this.name = name;
            this.channel = channel;
            this.ch = ch;
            this.refer = refer;
        }

        @Override public String toString() { return null; }
    }

    public interface OnResult {
        
        void run(boolean ok, String msg);
    }

    private ArkSmart() { }

    

    public static Kind classify(String arkJson) { return null; }

    public static String badge(String arkJson) { return null; }

    

    
    public static void send(final Context c, final String arkJson, final OnResult cb) {  }

    public static boolean rawSend(String arkJson) { return false; }

    private static void done(final OnResult cb, final boolean ok, final String msg) {  }
}
