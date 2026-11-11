package com.cathelperawa.owo.app.core;

public enum LoadPriority {

    CRITICAL(0),

    EARLY(1),

    DEFERRED(2),

    BACKGROUND(3),

    MANUAL(-1);

    private final int rank;

    LoadPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
