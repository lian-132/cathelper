package com.cathelperawa.owo.app.cookie;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

import java.util.List;

public class CookieProvider extends ContentProvider {

    public static final String AUTHORITY = "com.cathelperawa.owo.app.cookie";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);

    public static final String C_PLATFORM = "platform";
    public static final String C_COOKIE = "cookie";
    public static final String C_SAVED_AT = "saved_at";
    public static final String C_LABEL = "label";
    public static final String C_LEN = "len";

    private static final String[] ALLOWED_PKGS = {
            "com.tencent.tim",
            "com.tencent.mobileqq"
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    private boolean isAllowedCaller() {
        try {
            Context ctx = getContext();
            if (ctx == null) return false;
            int callingUid = Binder.getCallingUid();

            if (callingUid == Process.myUid()) return true;

            PackageManager pm = ctx.getPackageManager();
            for (String pkg : ALLOWED_PKGS) {
                try {
                    if (pm.getPackageUid(pkg, 0) == callingUid) return true;
                } catch (Throwable ignore) {

                }
            }

            try {
                String[] pkgs = pm.getPackagesForUid(callingUid);
                if (pkgs != null) {
                    for (String p : pkgs) {
                        for (String allow : ALLOWED_PKGS) {
                            if (allow.equals(p)) return true;
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
            try {
                String cp = getCallingPackage();
                if (cp != null) {
                    for (String allow : ALLOWED_PKGS) {
                        if (allow.equals(cp)) return true;
                    }
                }
            } catch (Throwable ignore) {
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (!isAllowedCaller()) return null;
        Context ctx = getContext();
        if (ctx == null) return null;

        List<String> seg = uri.getPathSegments();
        if (seg == null || seg.isEmpty()) return null;

        String head = seg.get(0);

        if ("get".equals(head)) {
            String platform = seg.size() > 1 ? seg.get(1) : null;
            if (platform == null) return null;
            CookieStore.Entry e = CookieStore.get(ctx, platform);
            if (e == null) return null;
            MatrixCursor c = new MatrixCursor(
                    new String[]{C_PLATFORM, C_COOKIE, C_SAVED_AT, C_LABEL});
            c.addRow(new Object[]{e.platform, e.cookie, e.savedAt, e.label});
            return c;
        }

        if ("list".equals(head)) {
            MatrixCursor c = new MatrixCursor(
                    new String[]{C_PLATFORM, C_SAVED_AT, C_LABEL, C_LEN});
            try {
                for (CookieStore.Entry e : CookieStore.all(ctx)) {
                    c.addRow(new Object[]{e.platform, e.savedAt, e.label,
                            e.cookie == null ? 0 : e.cookie.length()});
                }
            } catch (Throwable ignore) {
            }
            return c;
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if (!isAllowedCaller()) return 0;
        Context ctx = getContext();
        if (ctx == null) return 0;
        List<String> seg = uri.getPathSegments();
        if (seg == null || seg.isEmpty()) return 0;
        String platform = null;
        if ("get".equals(seg.get(0))) {
            if (seg.size() > 1) platform = seg.get(1);
        } else {
            platform = seg.get(0);
        }
        if (platform == null || platform.length() == 0) return 0;
        return CookieStore.remove(ctx, platform) ? 1 : 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.cathelper.cookie";
    }
}
