package com.cathelperawa.owo.app.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class ExtLink {

    private ExtLink() {}

    public static final String REPO = "https://github.com/lian-132/cathelper";

    public static final String GROUP = "mqqapi://card/show_pslcard"
            + "?authSig=4UzG38v4S85aYufMrpaY4HM4Br5rIiK9O0O+pQVDnXJqzZ7vzRMqQGShHVnxznnu"
            + "&card_type=group&src_type=internal&uin=459547307"
            + "&version=1&wSourceSubID=40001";

    public static boolean open(Context c, String url) {
        if (c == null || url == null || url.length() == 0) return false;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            try {
                Toast.makeText(c, url.startsWith("mqqapi://")
                        ? "没找到 QQ，先装个 QQ 再试"
                        : "打不开：" + t, Toast.LENGTH_SHORT).show();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }
}
