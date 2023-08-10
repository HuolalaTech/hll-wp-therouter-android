package com.therouter.router.action

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by ZhangTao on 23/1/03.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ActionInterceptor(val actionName: String)