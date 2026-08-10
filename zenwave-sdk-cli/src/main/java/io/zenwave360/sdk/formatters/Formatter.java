package io.zenwave360.sdk.formatters;

import io.zenwave360.sdk.processors.GeneratedFilesProcessor;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

public interface Formatter extends GeneratedFilesProcessor {

    enum Formatters {
        palantir, spring, google
    }
    void format(GeneratedProjectFiles generatedProjectFiles);

    @Override
    default void process(GeneratedProjectFiles generatedProjectFiles) {
        format(generatedProjectFiles);
    }
}
