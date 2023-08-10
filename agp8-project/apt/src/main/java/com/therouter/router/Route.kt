package com.therouter.router

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * https://youtrack.jetbrains.com/issue/KT-12794
 * Kotlin 的 @Repeatable 不能注解到 Java class，得要到1.6才能支持，但是 Java 的却能注解到 Kotlin 代码，所以这个类只能写 Java 的
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@java.lang.annotation.Repeatable(value = Routes::class)
@Repeatable
annotation class Route(
    /**
     * 路由path，不限格式，建议是一个url，允许多个path对应同一个Activity
     */
    val path: String = "",
    /**
     * 自定义事件，一般用来打开目标页面后做一个执行动作，例如随机页面弹出广告弹窗
     */
    val action: String = "",
    /**
     * 页面描述，会被记录到路由表中，方便后期排查的时候知道每个path或Activity是什么业务
     */
    val description: String = "",
    /**
     * 页面参数，自动写入intent中，允许写在路由表中动态下发修改默认值，或通过路由跳转时代码传入
     */
    val params: Array<String> = []
)