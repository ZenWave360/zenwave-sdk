package io.zenwave360.generator.parsers;

import java.io.IOException;
import java.util.Map;

public interface Parser {
    Parser withProjectClassLoader(ClassLoader projectClassLoader);

    Map<String, Object> parse() throws IOException;
}
