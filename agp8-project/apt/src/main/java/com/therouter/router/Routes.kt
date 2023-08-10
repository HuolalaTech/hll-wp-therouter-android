package com.therouter.router

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by ZhangTao on 17/8/11.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class Routes(vararg val value: Route)