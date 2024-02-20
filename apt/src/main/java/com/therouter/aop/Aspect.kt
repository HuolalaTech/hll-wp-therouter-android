package com.therouter.aop

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 元注解，用于定义一个AOP切点
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Aspect(val point: Int, val className: String, val methodName: String)