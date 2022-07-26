package io.zenwave360.generator.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Documents {@link io.zenwave360.generator.generators.Generator} options, used for building help and documentation messages.
 */
@Documented
@Target({ TYPE })
@Retention(RUNTIME)
public @interface DocumentedPlugin {

    String value();
    String description() default "";

    String shortCode() default "";
}