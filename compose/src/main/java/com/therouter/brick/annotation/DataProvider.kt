package com.therouter.brick.annotation

import java.lang.annotation.Repeatable
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by ZhangTao on 17/8/11.
 */
@Repeatable(DataProviders::class)
@kotlin.annotation.Repeatable
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class DataProvider(
    val path: String,
    val async: Boolean = false,
    val fieldName: String = "",
    val priority: Int = 0
)

@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class DataProviders(vararg val value: DataProvider)