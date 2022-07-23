package io.zenwave360.generator.generators;

import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;
import java.util.Map;

public interface Generator {

    List<TemplateOutput> generate(Map<String, Object> contextModel);
}
