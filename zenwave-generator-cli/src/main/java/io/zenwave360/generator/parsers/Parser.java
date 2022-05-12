package io.zenwave360.generator.parsers;

import java.io.File;
import java.io.IOException;

public interface Parser {
    Model parse(File file) throws IOException;
}
