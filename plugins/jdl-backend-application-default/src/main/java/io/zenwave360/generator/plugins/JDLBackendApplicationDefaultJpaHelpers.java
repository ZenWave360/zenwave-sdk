package io.zenwave360.generator.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.Options;

import io.zenwave360.generator.options.PersistenceType;
import io.zenwave360.generator.utils.JSONPath;

public class JDLBackendApplicationDefaultJpaHelpers {

    private final JDLBackendApplicationDefaultGenerator generator;

    JDLBackendApplicationDefaultJpaHelpers(JDLBackendApplicationDefaultGenerator generator) {
        this.generator = generator;
    }
}
