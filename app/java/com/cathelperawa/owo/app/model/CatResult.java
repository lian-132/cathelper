package com.cathelperawa.owo.app.model;

public final class CatResult {

    

    
    public enum Fail {
        
        OK,
        
        HOST_MISSING,
        
        BAD_ARGS,
        
        NOT_READY,
        
        REJECTED,
        
        TIMEOUT,
        
        UNSUPPORTED,
        
        UNKNOWN
    }

    

    private final Fail fail;
    private final String note;
    private final CatChannel channel;
    private final Throwable cause;
    private final long msgId;

    private CatResult(Fail fail, String note, CatChannel channel,
                      Throwable cause, long msgId) {
        this.fail = fail == null ? Fail.UNKNOWN : fail;
        this.note = note;
        this.channel = channel;
        this.cause = cause;
        this.msgId = msgId;
    }

    

    

    public static CatResult success() {
        return new CatResult(Fail.OK, null, CatChannel.KERNEL, null, 0);
    }

    public static CatResult ok(CatChannel ch) {
        return new CatResult(Fail.OK, null, ch == null ? CatChannel.KERNEL : ch, null, 0);
    }

    public static CatResult ok(CatChannel ch, long msgId) {
        return new CatResult(Fail.OK, null, ch == null ? CatChannel.KERNEL : ch, null, msgId);
    }

    public static CatResult ok(CatChannel ch, String note) {
        return new CatResult(Fail.OK, note, ch == null ? CatChannel.KERNEL : ch, null, 0);
    }

    

    
    public static CatResult fail(String note) {
        return new CatResult(Fail.UNKNOWN, note, null, null, 0);
    }

    public static CatResult fail(String note, Throwable cause) {
        return new CatResult(Fail.UNKNOWN, note, null, cause, 0);
    }

    public static CatResult fail(Fail f, String note) {
        return new CatResult(f, note, null, null, 0);
    }

    public static CatResult fail(Fail f, String note, Throwable cause) {
        return new CatResult(f, note, null, cause, 0);
    }

    

    
    public static CatResult badArgs(String note) {
        return fail(Fail.BAD_ARGS, note);
    }

    public static CatResult hostMissing(String what) {
        return fail(Fail.HOST_MISSING, "找不到宿主符号: " + what);
    }

    public static CatResult notReady(String what) {
        return fail(Fail.NOT_READY, what + " 未就绪");
    }

    public static CatResult rejected(String note) {
        return fail(Fail.REJECTED, note);
    }

    public static CatResult timeout(String what) {
        return fail(Fail.TIMEOUT, what + " 无回包");
    }

    public static CatResult unsupported(String note) {
        return fail(Fail.UNSUPPORTED, note);
    }

    public static CatResult unknown(String note, Throwable cause) {
        return fail(Fail.UNKNOWN, note, cause);
    }

    

    
    public boolean ok() {
        return fail == Fail.OK;
    }

    public boolean failed() {
        return fail != Fail.OK;
    }

    public Fail fail() {
        return fail;
    }

    public String note() {
        return note;
    }

    public CatChannel channel() {
        return channel;
    }

    public Throwable cause() {
        return cause;
    }

    public long msgId() {
        return msgId;
    }

    public String shortReason() {
        StringBuilder sb = new StringBuilder();
        sb.append(fail.name());
        if (note != null && !note.isEmpty()) {
            sb.append('·').append(note);
        } else if (channel != null) {
            sb.append('·').append(channel.label);
        }
        return sb.toString();
    }

    

    
    public CatResult withNote(String extra) {
        if (extra == null || extra.isEmpty()) return this;
        String merged = (note == null || note.isEmpty()) ? extra : (note + " · " + extra);
        return new CatResult(fail, merged, channel, cause, msgId);
    }

    public CatResult orThrow() {
        if (!ok()) throw new CatApiException(this);
        return this;
    }

    public CatResult orElse(Runnable fallback) {
        if (!ok() && fallback != null) {
            try {
                fallback.run();
            } catch (Throwable t) {
                logW("[CatResult] 兜底动作失败: " + t);
            }
        }
        return this;
    }

    

    
    public interface Logger {
        void w(String msg);
    }

    private static volatile Logger logger = new Logger() {
        @Override public void w(String msg) {
            try {
                com.cathelperawa.owo.app.core.HookLog.w(msg);
            } catch (Throwable ignore) {
                
            }
        }
    };

    public static void setLogger(Logger l) {
        logger = l == null ? new Logger() {
            @Override public void w(String msg) {  }
        } : l;
    }

    private static void logW(String msg) {
        Logger l = logger;
        if (l != null) l.w(msg);
    }

    public CatResult orElseDefault(CatResult other) {
        return ok() ? this : (other == null ? this : other);
    }

    

    
    public boolean toBoolean() {
        return ok();
    }

    @Override
    public String toString() {
        return shortReason();
    }
}
