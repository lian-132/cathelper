package com.cathelperawa.owo.app.model;

public final class CatApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CatResult result;

    public CatApiException(CatResult result) {
        super(result == null ? "CatApi 调用失败" : result.shortReason(),
                result == null ? null : result.cause());
        this.result = result;
    }

    public CatResult result() {
        return result;
    }

    public CatResult.Fail fail() {
        return result == null ? CatResult.Fail.UNKNOWN : result.fail();
    }
}
