package com.cathelperawa.owo.app.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HookItem {

    String name();

    String path() default "未分组";

    String description() default "";

    boolean defaultEnabled() default false;

    int order() default 100;

    int minVersion() default 0;

    int maxVersion() default 0;

    LoadPriority priority() default LoadPriority.EARLY;
}
