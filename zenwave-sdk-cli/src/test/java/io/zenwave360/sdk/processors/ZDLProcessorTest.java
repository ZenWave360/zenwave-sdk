package io.zenwave360.sdk.processors;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.utils.JSONPath;

public class ZDLProcessorTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    private boolean containsEntity(List<Map> entities, String entityName) {
        return entities.stream().filter(e -> entityName.contentEquals((String) e.get("name"))).findFirst().isPresent();
    }

    @Test
    public void testProcessZDL_CopyAnnotation() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var attachmentInput = JSONPath.get(model, "$.zdl.inputs.AddressInputCopy", Map.of());
        var fields = JSONPath.get(attachmentInput, "$.fields", Map.of());
        Assertions.assertNotNull(fields);
        Assertions.assertEquals(5, fields.size());
    }

}
