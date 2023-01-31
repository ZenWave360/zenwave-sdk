package io.zenwave360.sdk.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.parsers.JDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;

public class JDLEntitiesToSchemasConverterTest {

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        return (Map) new JDLProcessor().process(model).get("jdl");
    }

    @Test
    public void testConvertEntityToSchema() throws Exception {
        JDLEntitiesToSchemasConverter converter = new JDLEntitiesToSchemasConverter().withIdType("string");
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity, model);
            Assertions.assertEquals(entity.get(converter.jdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(schemas));
    }

    @Test
    public void testConvertEntityToSchemaRelational() throws Exception {
        JDLEntitiesToSchemasConverter converter = new JDLEntitiesToSchemasConverter().withIdType("string");
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity, model);
            Assertions.assertEquals(entity.get(converter.jdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(schemas));
    }
}
