package io.zenwave360.sdk.generators;

import io.zenwave360.sdk.doc.DocumentedOption;

public abstract class AbstractZDLGenerator implements Generator {

    @DocumentedOption(description = "Java Models package name")
    public String basePackage = "io.example.domain.model";
}
