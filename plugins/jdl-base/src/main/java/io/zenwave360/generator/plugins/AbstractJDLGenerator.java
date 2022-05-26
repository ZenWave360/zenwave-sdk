package io.zenwave360.generator.plugins;

import com.jayway.jsonpath.JsonPath;
import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.parsers.Model;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class AbstractJDLGenerator implements GeneratorPlugin {

    @DocumentedOption(description = "Java Models package name")
    public String domainModelPackage = "io.example.domain.model";

    public String getDomainModelPackageFolder() {
        return this.domainModelPackage.replaceAll("\\.", "/");
    }

}
