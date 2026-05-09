package io.zenwave360.sdk.plugins.templates;

import io.zenwave360.sdk.plugins.AsyncAPIOpsGenerator;
import io.zenwave360.sdk.templating.OutputFormatType;

public class TerraformConfluentTemplates extends AsyncAPIOpsGenerator.Templates {

    public TerraformConfluentTemplates(AsyncAPIOpsGenerator generator) {
        super("io/zenwave360/sdk/plugins/AsyncAPIOpsGenerator");
        // These templates mirror TerraformKafka but emit Confluent provider resources.
        addTemplate(commonTemplates, "TerraformConfluent/versions.tf", "versions.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformConfluent/topics.tf", "topics.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformConfluent/schemas.tf", "schemas.tf", OutputFormatType.YAML, null, false);
        addTemplate(commonTemplates, "TerraformConfluent/acls.tf", "acls.tf", OutputFormatType.YAML, null, false);
    }
}
