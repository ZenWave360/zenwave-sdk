package io.zenwave360.sdk.plugins;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.processors.ZDLProcessor;

@DocumentedPlugin(value = "Prints to StdOut ZDL Model as JSON")
public class ZdlToJsonPlugin extends Plugin implements Processor {

    public ZdlToJsonPlugin() {
        withChain(ZDLParser.class, ZDLProcessor.class, ZdlToJsonPlugin.class);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        var zdl = contextModel.get("zdl");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(zdl);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
