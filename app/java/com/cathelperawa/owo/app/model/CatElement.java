package com.cathelperawa.owo.app.model;

public final class CatElement {

    public enum Kind {
        TEXT("text"),
        IMAGE("pic"),
        VOICE("ptt"),
        VIDEO("video"),
        FILE("file"),
        FACE("face"),
        CARD("ark"),            
        MARKET_FACE("market"),  
        GIFT("gift"),           
        REPLY("reply"),         
        GRAY("gray"),           
        FORWARD("fwd"),         
        LOCATION("loc"),
        UNKNOWN("unknown");

        public final String legacyName;

        Kind(String legacyName) {
            this.legacyName = legacyName;
        }

        public static Kind of(String legacy) {
            if (legacy == null) return UNKNOWN;
            for (Kind k : values()) {
                if (k.legacyName.equals(legacy) || k.name().equalsIgnoreCase(legacy)) return k;
            }
            return UNKNOWN;
        }
    }

    

    private final Kind kind;
    private final Object raw;

    private CatElement(Kind kind, Object raw) {
        this.kind = kind == null ? Kind.UNKNOWN : kind;
        this.raw = raw;
    }

    

    
    public static CatElement of(Object raw) {
        if (raw == null) return new CatElement(Kind.UNKNOWN, null);
        return new CatElement(detectKind(raw), raw);
    }

    public static CatElement of(Kind kind, Object raw) {
        return new CatElement(kind, raw);
    }

    

    
    public Kind kind() {
        return kind;
    }

    public Object raw() {
        return raw;
    }

    public boolean valid() {
        return raw != null && kind != Kind.UNKNOWN;
    }

    public boolean sendable() {
        switch (kind) {
            case TEXT:
            case IMAGE:
            case VOICE:
            case VIDEO:
            case FILE:
            case FACE:
            case CARD:
            case GIFT:      
            case REPLY:
                return true;
            default:
                return false;
        }
    }

    

    
    private static Kind detectKind(Object el) {
        try {
            if (com.cathelperawa.owo.app.util.Refl.field(el, "grayTipElement") != null) return Kind.GRAY;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "replyElement") != null) return Kind.REPLY;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "textElement") != null) return Kind.TEXT;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "picElement") != null) return Kind.IMAGE;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "arkElement") != null) return Kind.CARD;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "multiForwardMsgElement") != null) return Kind.FORWARD;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "fileElement") != null) return Kind.FILE;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "videoElement") != null) return Kind.VIDEO;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "pttElement") != null) return Kind.VOICE;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "faceElement") != null) return Kind.FACE;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "marketFaceElement") != null) return Kind.MARKET_FACE;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "shareLocationElement") != null) return Kind.LOCATION;
            if (com.cathelperawa.owo.app.util.Refl.field(el, "textGiftElement") != null) return Kind.GIFT;
            
            int et = com.cathelperawa.owo.app.util.Refl.i32(
                    com.cathelperawa.owo.app.util.Refl.field(el, "elementType"), -1);
            switch (et) {
                case 1: return Kind.TEXT;
                case 2: return Kind.IMAGE;
                case 3: return Kind.FILE;
                case 4: return Kind.VOICE;
                case 5: return Kind.VIDEO;
                case 6: return Kind.FACE;
                case 7: return Kind.REPLY;
                case 8: return Kind.GRAY;
                case 10: return Kind.CARD;
                case 16: return Kind.FORWARD;
                default: break;
            }
        } catch (Throwable ex) {
            com.cathelperawa.owo.app.core.HookLog.w("[CatElement] 类型判定失败: " + ex);
        }
        return Kind.UNKNOWN;
    }

    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatElement)) return false;
        CatElement e = (CatElement) o;
        return kind == e.kind && (raw == null ? e.raw == null : raw.equals(e.raw));
    }

    @Override
    public int hashCode() {
        return kind.hashCode() * 31 + (raw == null ? 0 : raw.hashCode());
    }

    @Override
    public String toString() {
        return "CatElement(" + kind.legacyName + ")";
    }
}
