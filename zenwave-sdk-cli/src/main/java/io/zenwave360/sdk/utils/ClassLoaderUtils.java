package io.zenwave360.sdk.utils;

/**
 * The class loader for project resources and classes named in options ({@code classpath:} specs, ZDL apis,
 * layouts, {@code templates "new ..."}, .hbs templates): CLI {@code --deps}, Maven {@code includeProjectClasspath}.
 * <p>
 * Set by {@code MainGenerator.generate} from {@code Plugin.getProjectClassLoader()} for the duration of the
 * generation, per thread: Maven parallel builds share this class across executions.
 * Only SDK code reads it, unlike the thread context class loader.
 */
public final class ClassLoaderUtils {

    @FunctionalInterface
    public interface Body<T, E extends Exception> {
        T call() throws E;
    }

    private static final ThreadLocal<ClassLoader> projectClassLoader = new ThreadLocal<>();

    private ClassLoaderUtils() {}

    /** The project class loader, or the SDK class loader when none is configured. */
    public static ClassLoader getClassLoader() {
        ClassLoader classLoader = projectClassLoader.get();
        return classLoader != null ? classLoader : ClassLoaderUtils.class.getClassLoader();
    }

    /** The configured project class loader, or null. For libraries that have their own default when none is given. */
    public static ClassLoader getProjectClassLoader() {
        return projectClassLoader.get();
    }

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, getClassLoader());
    }

    /**
     * Runs {@code body} with {@code classLoader} as project class loader, then restores the previous one.
     * A null {@code classLoader} keeps the current one (nested generations inherit it).
     */
    public static <T, E extends Exception> T withProjectClassLoader(ClassLoader classLoader, Body<T, E> body) throws E {
        if (classLoader == null) {
            return body.call();
        }
        ClassLoader previous = projectClassLoader.get();
        projectClassLoader.set(classLoader);
        try {
            return body.call();
        } finally {
            if (previous != null) {
                projectClassLoader.set(previous);
            } else {
                projectClassLoader.remove();
            }
        }
    }
}
