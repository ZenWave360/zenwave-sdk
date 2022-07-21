package io.zenwave360.generator;

import io.zenwave360.generator.templating.HandlebarsEngine;
import io.zenwave360.generator.templating.TemplateEngine;
import io.zenwave360.generator.templating.TemplateOutput;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

public interface GeneratorPlugin {

    enum RoleType {
        PROVIDER, CLIENT
    }

    List<TemplateOutput> generate(Map<String, Object> contextModel);
}
