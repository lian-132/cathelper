package com.cathelperawa.owo.app.core;

public enum FeatureStatus {

    UNKNOWN("待触发", 0xFFAAB6C6),

    LOADING("加载中", 0xFF7FA5E6),

    OK("正常", 0xFF4CAF7D),

    PARTIAL("部分可用", 0xFFE8A33D),

    FAILED("失败", 0xFFE05C5C),

    ERROR("异常", 0xFFC0392B),

    DISABLED("已关闭", 0xFF9FB0C7),

    UNSUPPORTED("宿主版本不支持", 0xFF8E9BAE);

    public final String label;

    public final int color;

    FeatureStatus(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public boolean isProblem() {
        return this == FAILED || this == ERROR;
    }

    public boolean isHealthy() {
        return this == OK;
    }

    public boolean isDetected() {
        return this != UNKNOWN;
    }
}
