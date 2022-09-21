package io.zenwave360.generator.parsers;

import java.io.File;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import io.zenwave360.jsonrefparser.$Refs;

public class Model extends AbstractMap<String, Object> {

    private File file;
    private $Refs refs;

    public Model(File file, $Refs refs) {
        this.file = file;
        this.refs = refs;
    }

    public $Refs getRefs() {
        return refs;
    }

    public Map<String, Object> model() {
        return (Map<String, Object>) this.refs.jsonContext.json();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return model().entrySet();
    }
}
