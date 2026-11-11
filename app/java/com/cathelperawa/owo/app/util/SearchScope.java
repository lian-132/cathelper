package com.cathelperawa.owo.app.util;

public enum SearchScope {

    DECLARED,

    SUPERCLASSES,

    HIERARCHY;

    public static final SearchScope DEFAULT = SUPERCLASSES;

    public boolean includesSuper() {
        return this != DECLARED;
    }

    public boolean includesInterfaces() {
        return this == HIERARCHY;
    }
}
