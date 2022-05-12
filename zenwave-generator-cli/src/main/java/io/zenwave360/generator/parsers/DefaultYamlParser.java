package io.zenwave360.generator.parsers;

import com.jayway.jsonpath.internal.JsonContext;
import io.zenwave360.jsonrefparser.$RefParser;
import io.zenwave360.jsonrefparser.$RefParserOptions;
import io.zenwave360.jsonrefparser.$Refs;
import io.zenwave360.jsonrefparser.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class DefaultYamlParser implements io.zenwave360.generator.parsers.Parser {

    @Override
    public Model parse(File file) throws IOException {
        $RefParser parser = new $RefParser(file).withOptions(new $RefParserOptions($RefParserOptions.OnCircular.SKIP));
        return new Model(file, parser.dereference().getRefs());
    }
}
