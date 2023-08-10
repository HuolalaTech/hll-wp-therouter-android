package com.therouter.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * https://youtrack.jetbrains.com/issue/KT-12794
 * Kotlin 的 @Repeatable 不能注解到 Java class，得要到1.6才能支持，但是 Java 的却能注解到 Kotlin 代码，所以这个类只能写 Java 的
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Repeatable(Routes.class)
@kotlin.annotation.Repeatable
public @interface Route {
    /**
     * 路由path，不限格式，建议是一个url，允许多个path对应同一个Activity
     */
    String path() default "";

    /**
     * 自定义事件，一般用来打开目标页面后做一个执行动作，例如随机页面弹出广告弹窗
     */
    String action() default "";

    /**
     * 页面描述，会被记录到路由表中，方便后期排查的时候知道每个path或Activity是什么业务
     */
    String description() default "";

    /**
     * 页面参数，自动写入intent中，允许写在路由表中动态下发修改默认值，或通过路由跳转时代码传入
     */
    String[] params() default {};
}