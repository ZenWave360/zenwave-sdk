package io.zenwave360.generator.processors;

import static io.zenwave360.generator.processors.JDLWithOpenApiProcessor.JDL_DEFAULT_PROPERTY;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.utils.JSONPath;

public class JDLProcessorTest {

    private Map<String, Object> loadJDL(String resource) throws IOException {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).withTargetProperty(JDL_DEFAULT_PROPERTY).parse();
        return new JDLProcessor().withTargetProperty(JDL_DEFAULT_PROPERTY).process(model);
    }

    private boolean containsEntity(List<Map> entities, String entityName) {
        return entities.stream().filter(e -> entityName.contentEquals((String) e.get("name"))).findFirst().isPresent();
    }

    @Test
    public void testProcessJDL_WithSemanticAnnotations() throws Exception {
        var model = loadJDL("classpath:io/zenwave360/generator/resources/jdl/orders-model-semantic-annotations.jdl");
        List entitiesWithCriteria = JSONPath.get(model, "$..[?(@.options.searchCriteriaObject)]");
        Assertions.assertFalse(entitiesWithCriteria.isEmpty());
        Assertions.assertEquals(2, entitiesWithCriteria.size());
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "Customer"));
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "CustomerOrder"));
    }

    @Test
    // @Disabled
    public void testProcessJDL_registry() throws Exception {
        var model = loadJDL("../examples/spring-boot-mongo-elasticsearch/src/main/resources/model/orders-model.jdl");
        List entitiesWithCriteria = JSONPath.get(model, "$..[?(@.options.searchCriteriaObject)]");
        Assertions.assertFalse(entitiesWithCriteria.isEmpty());
    }
}
