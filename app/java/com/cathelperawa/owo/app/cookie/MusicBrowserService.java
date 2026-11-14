package com.cathelperawa.owo.app.cookie;

import android.os.Bundle;
import android.service.media.MediaBrowserService;

import java.util.ArrayList;
import java.util.List;

public class MusicBrowserService extends MediaBrowserService {

    private static final String ROOT_ID = "cathelper_root";

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {

        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(String parentId,
                               Result<List<android.media.browse.MediaBrowser.MediaItem>> result) {

        result.sendResult(new ArrayList<android.media.browse.MediaBrowser.MediaItem>());
    }
}
