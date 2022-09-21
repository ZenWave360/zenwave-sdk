package io.zenwave360.generator.processors;

import java.util.Map;

public interface Processor {

    Map<String, Object> process(Map<String, Object> contextModel);

}
