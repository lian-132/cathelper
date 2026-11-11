package com.cathelperawa.owo.app.feature;

import java.util.LinkedHashMap;

public final class FakeMap extends LinkedHashMap<Object, Object> {
    private static final int LIE_AT = 9;
    private static final int LIE_TO = 8;

    @Override 
    public int size() {
        int size = super.size();
        if (size == 9) {
            return 8;
        }
        return size;
    }
}
