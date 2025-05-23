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

    String title() default "";

    String summary() default "";

    String description() default "";

    String[] mainOptions() default {};

    String[] hiddenOptions() default {};
}
