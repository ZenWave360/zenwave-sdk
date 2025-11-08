package io.zenwave360.sdk.plugins.templates;

import io.zenwave360.sdk.generators.AbstractAsyncapiGenerator;
import io.zenwave360.sdk.plugins.AsyncAPIGenerator;
import io.zenwave360.sdk.templating.HandlebarsEngine;

public class SpringCloudStreamTemplates extends AbstractAsyncapiGenerator.Templates {

    public SpringCloudStreamTemplates(AsyncAPIGenerator generator) {
        super("io/zenwave360/sdk/plugins/AsyncAPIGenerator");

        HandlebarsEngine handlebarsEngine = (HandlebarsEngine) generator.getTemplateEngine();
        handlebarsEngine.registerHelpers(new AsyncAPIHandlebarsHelpers(generator));

        addTemplate(producerTemplates, "shared/producer/mocks/EventsProducerInMemoryContext.java", "src/test/java/{{asPackageFolder producerApiPackage}}/EventsProducerInMemoryContext.java");

        addTemplate(producerByServiceTemplates, "shared/producer/IProducer.java", "src/main/java/{{asPackageFolder producerApiPackage}}/{{producerInterfaceName serviceName operationRoleType}}.java");
        if(generator.transactionalOutbox == AsyncAPIGenerator.TransactionalOutboxType.none) {
            addTemplate(producerByServiceTemplates, "scs/producer/outbox/none/Producer.java", "src/main/java/{{asPackageFolder producerApiPackage}}/{{producerClassName serviceName operationRoleType}}.java");
        } else {
            addTemplate(producerByServiceTemplates, "shared/producer/outbox/{{transactionalOutbox}}/Producer.java", "src/main/java/{{asPackageFolder producerApiPackage}}/{{producerClassName serviceName operationRoleType}}.java");
        }
        addTemplate(producerByServiceTemplates, "shared/producer/mocks/InMemoryEventsProducer.java", "src/test/java/{{asPackageFolder producerApiPackage}}/{{producerInMemoryName serviceName operationRoleType}}.java");

        addTemplate(consumerByOperationTemplates, "scs/consumer/{{style}}/Consumer.java", "src/main/java/{{asPackageFolder consumerApiPackage}}/{{consumerName operation.x--operationIdCamelCase}}.java");
        addTemplate(consumerByOperationTemplates, "shared/consumer/{{style}}/IService.java", "src/main/java/{{asPackageFolder consumerApiPackage}}/{{consumerServiceInterfaceName operation.x--operationIdCamelCase}}.java");

    }
}
