package com.eka.middleware.sdk.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={METHOD, CONSTRUCTOR})
public @interface SyncloopFunction {

    String title() default "";

    String description() default "";

    String[] in() default {};
    String out() default "";
}