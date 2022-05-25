package io.zenwave360.generator.parsers;

import java.io.IOException;
import java.util.Map;

public interface Parser {
    Map<String, Model> parse() throws IOException;
}
