package io.zenwave360.sdk.parsers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface Parser {
    Parser withProjectClassLoader(ClassLoader projectClassLoader);

    Map<String, Object> parse() throws IOException;

    default String loadSpecFile(String specFile) {
        if (specFile.startsWith("classpath:")) {
            try {
                return new String(getClass().getClassLoader().getResourceAsStream(specFile.replaceFirst("classpath:", "")).readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return Files.readString(Paths.get(specFile), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    class ParseException extends RuntimeException {
        public ParseException(Throwable t) {
            super(t);
        }
    }

    class ParseProblemsException extends RuntimeException {
        final List<Object> problems;
        public ParseProblemsException(List<Object> problems) {
            this.problems = problems;
        }
    }
}
