package io.zenwave360.sdk.plugins.templates;

import io.zenwave360.sdk.plugins.AsyncAPIOpsGenerator;

public class TerraformKafkaTemplates extends AsyncAPIOpsGenerator.Templates {

    public TerraformKafkaTemplates(AsyncAPIOpsGenerator generator) {
        super("io/zenwave360/sdk/plugins/AsyncAPIOpsGenerator");
        addTemplate(allChannelsTemplates, "TerraformKafka/topics.tf",
                "topics.tf");

        addTemplate(messageTemplates, "TerraformKafka/schemas.tf",
                "schema-{{channelName}}-{{messageName}}.tf");
    }
}
