package io.zenwave360.sdk.formatters;

import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public interface Formatter {

    enum Formatters {
        palantir, spring, google
    }
    void format(GeneratedProjectFiles generatedProjectFiles);
}
