package com.cathelperawa.owo.app.cookie;

import android.app.Activity;
import android.os.Bundle;

public class CookieWarmupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            finish();
        } catch (Throwable ignore) {
        }
    }
}
