package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.GeneratorPlugin;


public abstract class AbstractJDLGenerator implements GeneratorPlugin {

    @DocumentedOption(description = "Java Models package name")
    public String basePackage = "io.example.domain.model";

    public String getBasePackageFolder() {
        return this.basePackage.replaceAll("\\.", "/");
    }

}
