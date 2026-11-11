package com.cathelperawa.owo.app.model;

public enum CatChannel {

    
    KERNEL("kernel"),

    
    DIRECT("direct"),
    
    LONG("long"),
    
    CARD("card"),

    
    GRAB("grab"),
    
    TEXT("text"),

    
    GRAY_TIP("grayTip"),
    
    GHOST("ghost"),

    
    HB_NATIVE("hbNative"),
    
    NET("net"),
    
    SERVLET("servlet");

    public final String label;

    CatChannel(String label) {
        this.label = label;
    }

    public static CatChannel of(String label) {
        if (label == null) return null;
        for (CatChannel c : values()) {
            if (c.label.equals(label) || c.name().equalsIgnoreCase(label)) return c;
        }
        return null;
    }

    @Override
    public String toString() {
        return label;
    }
}
