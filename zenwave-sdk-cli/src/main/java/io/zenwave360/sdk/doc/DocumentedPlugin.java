package io.zenwave360.sdk.doc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Documents {@link io.zenwave360.sdk.generators.Generator} options, used for building help and documentation messages.
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface DocumentedPlugin {

    String value();

    String description() default "";

    String shortCode() default "";

    String[] hiddenOptions() default {};
}
