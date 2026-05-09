package io.zenwave360.sdk.plugins.templates;

import io.zenwave360.sdk.plugins.AsyncAPIOpsGenerator;
import io.zenwave360.sdk.templating.OutputFormatType;

public class TerraformConfluentHybridTemplates extends AsyncAPIOpsGenerator.Templates {

    public TerraformConfluentHybridTemplates(AsyncAPIOpsGenerator generator) {
        super("io/zenwave360/sdk/plugins/AsyncAPIOpsGenerator");
        // Hybrid setup keeps Confluent topic and ACL resources, but uses the standalone Schema Registry provider.
        addTemplate(commonTemplates, "TerraformConfluentHybrid/versions.tf", "versions.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformConfluent/topics.tf", "topics.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformKafka/schemas.tf", "schemas.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformConfluent/acls.tf", "acls.tf", OutputFormatType.YAML, null, false);
    }
}
