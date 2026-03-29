package io.zenwave360.sdk.parsers;

public interface WithProjectClassLoader<T> {
    T withProjectClassLoader(ClassLoader projectClassLoader);
}
