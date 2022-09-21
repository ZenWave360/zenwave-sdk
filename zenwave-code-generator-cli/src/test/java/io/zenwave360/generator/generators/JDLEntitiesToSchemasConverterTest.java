package io.zenwave360.generator.generators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.generator.parsers.JDLParser;
import io.zenwave360.generator.processors.JDLProcessor;
import io.zenwave360.generator.utils.JSONPath;

public class JDLEntitiesToSchemasConverterTest {

    private Map<String, Object> loadJDLModelFromResource(String resource) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(resource).toURI());
        Map<String, Object> model = new JDLParser().withSpecFile(file.getAbsolutePath()).parse();
        return (Map) new JDLProcessor().process(model).get("jdl");
    }

    @Test
    public void testConvertEntityToSchema() throws Exception {
        JDLEntitiesToSchemasConverter converter = new JDLEntitiesToSchemasConverter().withIdType("string");
        Map<String, Object> model = loadJDLModelFromResource("io/zenwave360/generator/resources/jdl/orders-model.jdl");
        List<Map> entities = JSONPath.get(model, "entities[*]");
        List<Map> enums = JSONPath.get(model, "enums.enums[*]");
        List<Map> entitiesAndEnums = new ArrayList<>();
        entitiesAndEnums.addAll(entities);
        entitiesAndEnums.addAll(enums);

        List<Map> schemas = new ArrayList<>();

        for (Map entity : entitiesAndEnums) {
            Map<String, Object> result = converter.convertToSchema(entity);
            Assertions.assertEquals(entity.get(converter.jdlBusinessEntityProperty), result.get("name"));
            schemas.add(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(schemas));
    }
}
