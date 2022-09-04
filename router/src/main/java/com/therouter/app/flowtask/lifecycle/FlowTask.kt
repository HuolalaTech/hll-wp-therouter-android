package com.therouter.app.flowtask.lifecycle

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class FlowTask(val taskName: String, val dependsOn: String = "", val async: Boolean = false)