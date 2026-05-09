package io.zenwave360.sdk.plugins.templates;

import io.zenwave360.sdk.plugins.AsyncAPIOpsGenerator;
import io.zenwave360.sdk.templating.OutputFormatType;

public class TerraformKafkaTemplates extends AsyncAPIOpsGenerator.Templates {

    public TerraformKafkaTemplates(AsyncAPIOpsGenerator generator) {
        super("io/zenwave360/sdk/plugins/AsyncAPIOpsGenerator");
        // All three templates receive the full intent model via commonTemplates.
        // OutputFormatType.YAML bypasses JavaFormatter (which only processes JAVA/KOTLIN).
        // Handlebars ClassPathTemplateLoader appends .hbs automatically — do NOT include .hbs in the path here.
        addTemplate(commonTemplates, "TerraformKafka/versions.tf",    "versions.tf",    OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformKafka/topics.tf",  "topics.tf",  OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformKafka/schemas.tf", "schemas.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformKafka/acls.tf",    "acls.tf",    OutputFormatType.YAML, null, false);
    }
}
