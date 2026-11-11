package com.cathelperawa.owo.app.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class ReportProvider extends ContentProvider {

    public static final String AUTHORITY = "com.cathelperawa.owo.app.report";

    private static final String SUB_DIR = "bugreport";

    public static Uri uriFor(File f) {

        return Uri.parse("content://" + AUTHORITY + "/" + f.getName());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File resolve(Uri uri) {
        if (uri == null) return null;
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty()) return null;
        
        if (name.contains("/") || name.contains("\\") || name.contains("..")) return null;
        Context ctx = getContext();
        if (ctx == null) return null;
        File dir = new File(ctx.getCacheDir(), SUB_DIR);
        File f = new File(dir, name);
        try {
            
            if (!f.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
        return f.isFile() ? f : null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = resolve(uri);
        if (f == null) throw new FileNotFoundException(String.valueOf(uri));
        
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File f = resolve(uri);
        if (f == null) return null;
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
            else row[i] = null;
        }
        c.addRow(row);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "application/zip";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;    
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;       
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;       
    }
}
