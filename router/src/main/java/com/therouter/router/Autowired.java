package com.therouter.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for parameters, which need autowired.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface Autowired {

    String name() default "";

    String args() default "";

    int id() default 0;

    // If required, app will be crash when value is null.
    // Primitive type wont be check!
    boolean required() default false;

    // Description of the field
    String description() default "No desc.";
}
