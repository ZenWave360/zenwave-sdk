package io.zenwave360.sdk.templating;

import com.github.jknack.handlebars.Options;
import io.zenwave360.sdk.utils.AsyncAPIUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Handlebars helpers for properties added to the model by the AsyncAPI processing pipeline. */
public final class AsyncapiHandlebarsHelpers {

    private final Supplier<Map<String, Object>> apiModel;

    public AsyncapiHandlebarsHelpers(Supplier<Map<String, Object>> apiModel) {
        this.apiModel = apiModel;
    }

    /** Returns the messages associated with an AsyncAPI operation. */
    public List<Map<String, Object>> operationMessages(Object operationId, Options options) {
        Map<String, Object> model = apiModel.get();
        return model == null || operationId == null
                ? List.of()
                : AsyncAPIUtils.operationMessages(model, operationId.toString());
    }

    /** Returns the generated Java type stored on an AsyncAPI message. */
    public String javaType(Object message, Options options) {
        return stringProperty(message, "x--javaType");
    }

    /** Returns the simple generated Java type stored on an AsyncAPI message. */
    public String javaTypeSimpleName(Object message, Options options) {
        return stringProperty(message, "x--javaTypeSimpleName");
    }

    /** Returns the originating {@code components.schemas} name stored on a schema. */
    public String schemaName(Object schema, Options options) {
        return stringProperty(schema, "x--schema-name");
    }

    private static String stringProperty(Object value, String property) {
        return value instanceof Map<?, ?> map ? (String) map.get(property) : null;
    }
}
