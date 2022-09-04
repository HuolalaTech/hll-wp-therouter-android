package com.therouter.inject

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Created by ZhangTao on 17/8/23.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class Singleton 