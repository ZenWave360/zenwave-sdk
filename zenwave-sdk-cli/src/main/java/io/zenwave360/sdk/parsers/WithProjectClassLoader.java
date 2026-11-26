package io.zenwave360.sdk.parsers;

/**
 * @deprecated the project class loader is set once per generation in {@link io.zenwave360.sdk.utils.ClassLoaderUtils}
 * by {@code MainGenerator}; read it from there. SDK implementations ignore this call. Still invoked by
 * {@code MainGenerator} for implementations that override it.
 */
@Deprecated
public interface WithProjectClassLoader<T> {
    @Deprecated
    @SuppressWarnings("unchecked")
    default T withProjectClassLoader(ClassLoader projectClassLoader) {
        return (T) this;
    }
}
