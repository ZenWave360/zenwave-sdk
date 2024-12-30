package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class PathsProcessorTest {


    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        model = new ZDLProcessor().process(model);
        return model;
    }

    @Test
    public void test_process_inline_parameters() throws Exception {
        Map<String, Object> model = loadZDLModelFromResource("classpath:inline-parameters.zdl");
        model = new PathsProcessor().process(model);
        List<Map<String, Object>> paths = (List<Map<String, Object>>) model.get("paths");
    }
}
