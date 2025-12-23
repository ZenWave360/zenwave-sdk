package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.zenwave360.sdk.parsers.Parser;
import io.zenwave360.sdk.parsers.ZDLParser;

public class EventCatalogGeneratorTest {

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Parser parser = new ZDLParser();

}
