package com.therouter.app.flowtask

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 元注解，用于定义一个初始化任务
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class VirtualFlowTask(val taskName: String)