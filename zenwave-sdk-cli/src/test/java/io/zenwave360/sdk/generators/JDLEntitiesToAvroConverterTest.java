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

public class JDLEntitiesToAvroConverterTest {

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new JDLParser().withSpecFile(resource).parse();
        return (Map) new JDLProcessor().process(model).get("jdl");
    }

    @Test
    public void testConvertEntityToSchema() throws Exception {
        JDLEntitiesToAvroConverter converter = new JDLEntitiesToAvroConverter().withIdType("string").withNamespace("io.example");
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> avros = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToAvro(entity, model);
            Assertions.assertEquals(entity.get("name"), result.get("name"));
            avros.add(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        for (Map avro : avros) {
            String className = String.format("%s.%s", converter.namespace, avro.get("name"));
            System.out.println("------- " + className + " ----------");
            System.out.println(mapper.writeValueAsString(avro));
        }
    }

    @Test
    public void testConvertEntityToSchemaRelational() throws Exception {
        JDLEntitiesToAvroConverter converter = new JDLEntitiesToAvroConverter().withIdType("string").withNamespace("io.example");
        Map<String, Object> model = loadJDLModelFromResource("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> avros = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToAvro(entity, model);
            Assertions.assertEquals(entity.get("name"), result.get("name"));
            avros.add(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        for (Map avro : avros) {
            String className = String.format("%s.%s", converter.namespace, avro.get("name"));
            System.out.println("------- " + className + " ----------");
            System.out.println(mapper.writeValueAsString(avro));
        }
    }
}
