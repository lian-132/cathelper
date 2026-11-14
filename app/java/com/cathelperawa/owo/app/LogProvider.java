package com.cathelperawa.owo.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

public class LogProvider extends ContentProvider {

    public static final String AUTHORITY = "com.cathelperawa.owo.app.logs";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY);
    public static final String FILE_NAME = "module_log.txt";
    public static final String MSG_FILE = "module_msg.txt";
    private static final long MAX_SIZE = 128 * 1024;

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
            android.content.Context ctx = getContext();
            if (ctx == null) return false;
            int callingUid = android.os.Binder.getCallingUid();

            if (callingUid == android.os.Process.myUid()) return true;

            android.content.pm.PackageManager pm = ctx.getPackageManager();
            for (String pkg : ALLOWED_PKGS) {
                try {
                    int uid = pm.getPackageUid(pkg, 0);
                    if (uid == callingUid) return true;
                } catch (Throwable ignore) {

                }
            }

            String callingPkg = null;
            try {
                String[] pkgs = pm.getPackagesForUid(callingUid);
                if (pkgs != null && pkgs.length > 0) callingPkg = pkgs[0];
            } catch (Throwable ignore) {
            }
            if (callingPkg != null) {
                for (String pkg : ALLOWED_PKGS) {
                    if (pkg.equals(callingPkg)) return true;
                }
            }

            try {
                String cp = getCallingPackage();
                if (cp != null) {
                    for (String pkg : ALLOWED_PKGS) {
                        if (pkg.equals(cp)) return true;
                    }
                }
            } catch (Throwable ignore) {
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static File logFile(android.content.Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (!isAllowedCaller()) return null;
        try {
            android.content.Context ctx = getContext();
            if (ctx == null || values == null) return null;
            String tag = values.getAsString("tag");
            String msg = values.getAsString("msg");
            if (TextUtils.isEmpty(msg)) return null;

            File f = new File(ctx.getFilesDir(),
                    "MSG".equals(tag) ? MSG_FILE : FILE_NAME);

            if (f.exists() && f.length() > MAX_SIZE) {
                try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                    long keep = raf.length() / 2;
                    byte[] buf = new byte[(int) keep];
                    raf.seek(raf.length() - keep);
                    raf.readFully(buf);
                    raf.setLength(0);
                    raf.write(buf);
                }
            }

            String line = "[" + new java.text.SimpleDateFormat("MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date()) + "] "
                    + (TextUtils.isEmpty(tag) ? "" : tag + " ") + msg + "\n";
            try (FileOutputStream fo = new FileOutputStream(f, true)) {
                fo.write(line.getBytes("UTF-8"));
            }
        } catch (Throwable ignore) {
        }
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if (!isAllowedCaller()) return 0;
        try {
            File f = logFile(getContext());
            if (f.exists()) f.delete();
        } catch (Throwable ignore) {
        }
        return 1;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (!isAllowedCaller()) return 0;
        if (values == null || values.length == 0) return 0;
        int n = 0;
        for (ContentValues v : values) {
            if (v == null) continue;
            try {
                if (insert(uri, v) != null) n++;
            } catch (Throwable ignore) {
            }
        }
        return n;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }
}
