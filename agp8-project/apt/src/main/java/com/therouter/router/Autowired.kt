package com.therouter.router

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Annotation for parameters, which need autowired.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class Autowired(
    val name: String = "",
    val args: String = "",
    val id: Int = 0,  // If required, app will be crash when value is null.
    // Primitive type wont be check!
    val required: Boolean = false,  // Description of the field
    val description: String = "No desc."
)