package io.zenwave360.sdk.doc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Documents {@link io.zenwave360.sdk.generators.Generator} options, used for building help and documentation messages.
 */
@Documented
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
public @interface DocumentedOption {

    String description() default "";

    boolean required() default false;

    String defaultValue() default "";

    String[] values() default {};

    String docLink() default "";
}
