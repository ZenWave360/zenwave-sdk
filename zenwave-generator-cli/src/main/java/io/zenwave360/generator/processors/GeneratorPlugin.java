package io.zenwave360.generator.processors;

import io.zenwave360.generator.templating.TemplateOutput;

import java.util.List;
import java.util.Map;

public interface GeneratorPlugin {

    public List<TemplateOutput> generate(Map<String, Object> apiModel);
}
