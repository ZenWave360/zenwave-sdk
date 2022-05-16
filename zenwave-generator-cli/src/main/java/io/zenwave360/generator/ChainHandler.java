package io.zenwave360.generator;

import io.zenwave360.generator.parsers.Model;

import java.util.Map;

public interface ChainHandler {

    Model chain(Model model);

    default Model mixModels(Model parentModel, Map<String, Object> childModel, String targetProperty) {
        if (targetProperty != null) {
            parentModel.put(targetProperty, childModel);
        } else {
            parentModel.putAll(childModel);
        }
        return parentModel;
    }
}
