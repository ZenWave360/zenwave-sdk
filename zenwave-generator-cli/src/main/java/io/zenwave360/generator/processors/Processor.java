package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.Model;

import java.util.Map;

public interface Processor {

    public String targetProperty = "api";

    Map<String, ?> process(Map<String, ? extends Object> model);
}
