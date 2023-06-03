package io.zenwave360.sdk.processors;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLProcessorTest {

    private Map<String, Object> loadJDL(String resource) throws IOException {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        return new JDLProcessor().process(model);
    }

    private boolean containsEntity(List<Map> entities, String entityName) {
        return entities.stream().filter(e -> entityName.contentEquals((String) e.get("name"))).findFirst().isPresent();
    }

    @Test
    public void testProcessJDL_WithSemanticAnnotations() throws Exception {
        var model = loadJDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        List entitiesWithCriteria = JSONPath.get(model, "$.jdl.entities[*][?(@.options.searchCriteriaObject)]");
        Assertions.assertFalse(entitiesWithCriteria.isEmpty());
        Assertions.assertEquals(2, entitiesWithCriteria.size());
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "Customer"));
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "CustomerOrder"));
    }

    @Test
    public void testProcessJDL_Relational() throws Exception {
        var model = loadJDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        List entitiesWithCriteria = JSONPath.get(model, "$.jdl.entities[*][?(@.options.searchCriteriaObject)]");
        Assertions.assertFalse(entitiesWithCriteria.isEmpty());
        Assertions.assertEquals(2, entitiesWithCriteria.size());
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "Customer"));
        Assertions.assertTrue(containsEntity(entitiesWithCriteria, "CustomerOrder"));
    }

}
