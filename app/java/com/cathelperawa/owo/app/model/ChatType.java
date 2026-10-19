package com.cathelperawa.owo.app.model;

public enum ChatType {

    FRIEND(1),

    GROUP(2),

    SERVICE(100),

    UNKNOWN(0);

    public final int code;

    ChatType(int code) {
        this.code = code;
    }

    public static ChatType of(int code) {
        for (ChatType t : values()) {
            if (t.code == code) return t;
        }
        return UNKNOWN;
    }

    public boolean isGroup() {
        return this == GROUP;
    }

    public boolean isFriend() {
        return this == FRIEND;
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    @Override
    public String toString() {
        return name() + "(" + code + ")";
    }
}
