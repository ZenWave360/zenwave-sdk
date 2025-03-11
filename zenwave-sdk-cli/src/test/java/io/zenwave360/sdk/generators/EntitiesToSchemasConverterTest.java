package io.zenwave360.sdk.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;

public class EntitiesToSchemasConverterTest {

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    public void testConvertEntityToSchema() throws Exception {
        EntitiesToSchemasConverter converter = new EntitiesToSchemasConverter().withIdType("string");
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity, model);
            Assertions.assertEquals(entity.get(converter.zdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

//        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(mapper.writeValueAsString(schemas));
    }

    @Test
    public void testConvertEntityToSchemaRelational() throws Exception {
        EntitiesToSchemasConverter converter = new EntitiesToSchemasConverter().withIdType("number", "int64");
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity, model);
            Assertions.assertEquals(entity.get(converter.zdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

//        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(mapper.writeValueAsString(schemas));
    }

    @Test
    public void testConvertEntityToSchemaFullEquipped() throws Exception {
        EntitiesToSchemasConverter converter = new EntitiesToSchemasConverter().withIdType("number", "int64");
        Map<String, Object> model = loadZDLModelFromResource("classpath:io/zenwave360/sdk/zdl/populate-fields.zdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity, model);
            Assertions.assertEquals(entity.get(converter.zdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

//        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(mapper.writeValueAsString(schemas));
    }
}
