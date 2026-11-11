package com.cathelperawa.owo.app.script;

public final class ScriptScope {

    public static final int ALL = 0;
    
    public static final int GROUP = 1;
    
    public static final int FRIEND = 2;

    private ScriptScope() {}

    public static int parse(String code) {
        if (code == null) return ALL;
        String head = code.length() > 2000 ? code.substring(0, 2000) : code;
        int i = head.indexOf("scope:");
        if (i < 0) return ALL;
        int end = i + 6;
        int lineEnd = head.indexOf('\n', end);
        if (lineEnd < 0) lineEnd = head.length();
        String body = head.substring(end, lineEnd)
                .replace("*/", "").replace("//", "").trim().toLowerCase();

        boolean group = body.contains("group");
        boolean friend = body.contains("friend");
        if (group && friend) return ALL;
        if (group) return GROUP;
        if (friend) return FRIEND;
        return ALL;
    }

    public static boolean allow(int scope, boolean isGroup) {
        if (scope == ALL) return true;
        if (scope == GROUP) return isGroup;
        if (scope == FRIEND) return !isGroup;
        return true;
    }

    public static String label(int scope) {
        if (scope == GROUP) return "只群聊";
        if (scope == FRIEND) return "只私聊";
        return "";
    }
}
