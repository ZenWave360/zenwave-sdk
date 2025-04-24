package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.plugins.eventproducer.EventProducer;
import org.junit.jupiter.api.Test;

public class JavaToAsyncAPIGeneratorTest {

    @Test
    public void test_event_producer_to_asyncapi() throws Exception {
        String asyncapi = new JavaToAsyncAPIGenerator()
                .withEventProducerClass(EventProducer.class)
                .withAsyncapiVersion(AsyncapiVersionType.v3)
                .withTargetFile("target/out/asyncapi.yml")
//                .withDebugZdl(true)
                .generate();
//        System.out.println(asyncapi);
    }

    @Test
    public void test_event_producer_to_asyncapi_avro() throws Exception {
        String asyncapi = new JavaToAsyncAPIGenerator()
                .withEventProducerClass(EventProducer.class)
                .withTargetFile("target/out/asyncapi-avro.yml")
                .withSchemaFormat(JavaToAsyncAPIGenerator.SchemaFormat.avro)
//                .withDebugZdl(true)
                .generate();
//        System.out.println(asyncapi);
    }
}
