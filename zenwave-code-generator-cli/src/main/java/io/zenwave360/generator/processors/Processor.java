package io.zenwave360.generator.processors;

import io.zenwave360.generator.parsers.Model;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public interface Processor {

    Map<String, ?> process(Map<String, ? extends Object> contextModel);

}
