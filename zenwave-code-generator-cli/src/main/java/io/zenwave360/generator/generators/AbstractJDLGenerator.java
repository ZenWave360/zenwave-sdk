package io.zenwave360.generator.generators;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.generators.Generator;


public abstract class AbstractJDLGenerator implements Generator {

    @DocumentedOption(description = "Java Models package name")
    public String basePackage = "io.example.domain.model";
}
