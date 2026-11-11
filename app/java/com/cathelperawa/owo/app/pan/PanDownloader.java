package com.cathelperawa.owo.app.pan;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public final class PanDownloader {

    private PanDownloader() {}

    public static final String FOLDER = "猫娘网盘";

    public static long enqueue(Context ctx, PanProvider.Link link, String platformName) throws Exception {
        if (ctx == null || link == null || link.url == null || link.url.length() == 0) {
            throw new IllegalArgumentException("直链为空");
        }
        DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) throw new IllegalStateException("系统下载服务不可用");

        Uri uri;
        try {
            uri = Uri.parse(link.url);
        } catch (Throwable t) {
            throw new IllegalArgumentException("直链格式不对");
        }
        DownloadManager.Request r = new DownloadManager.Request(uri);
        String name = safeName(link.filename, uri.getLastPathSegment());
        r.setTitle(name);
        r.setDescription(platformName + " · " + PanItem.human(link.size));
        if (link.ua != null && link.ua.length() > 0) r.addRequestHeader("User-Agent", link.ua);
        if (link.referer != null && link.referer.length() > 0) r.addRequestHeader("Referer", link.referer);
        if (link.cookie != null && link.cookie.length() > 0) r.addRequestHeader("Cookie", link.cookie);
        String dir = PanPrefs.subDir(ctx);
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        r.setAllowedOverRoaming(true);
        r.setAllowedOverMetered(true);
        try {
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, dir + "/" + name);
        } catch (Throwable t) {
            
            r.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, dir + "/" + name);
        }
        return dm.enqueue(r);
    }

    public static String safeName(String name, String fallback) {
        String n = name == null || name.trim().length() == 0 ? fallback : name.trim();
        if (n == null || n.length() == 0) n = "pan_" + System.currentTimeMillis();
        n = n.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_");
        if (n.length() > 120) {
            String ext = "";
            int dot = n.lastIndexOf('.');
            if (dot > 0 && n.length() - dot <= 8) ext = n.substring(dot);
            n = n.substring(0, 120 - ext.length()) + ext;
        }
        return n;
    }
}
