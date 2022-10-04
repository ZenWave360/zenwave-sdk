package io.zenwave360.example.config;

import io.zenwave360.example.core.outbound.events.CustomerEventsProducer;
import io.zenwave360.example.core.outbound.events.CustomerOrderEventsProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.*;

import java.util.Map;

@Configuration
public class OutboxConfig {

    @Bean(destroyMethod = "stop")
    public MessageListenerContainer configOutboxChangeStreams(MongoTemplate template, CustomerEventsProducer customerEventsProducer, CustomerOrderEventsProducer customerOrderEventsProducer) {
        MessageListenerContainer container = new DefaultMessageListenerContainer(template);
        ChangeStreamRequest.ChangeStreamRequestOptions options = new ChangeStreamRequest.ChangeStreamRequestOptions(
                null, customerEventsProducer.onCustomerEventBindingName, ChangeStreamOptions.empty());

        // Register all listeners for the outbox collections here
        container.register(new ChangeStreamRequest<>(customerEventsProducer.onCustomerEventMongoChangeStreamsListener, options), Map.class);
        container.register(new ChangeStreamRequest<>(customerOrderEventsProducer.onCustomerOrderEventMongoChangeStreamsListener, options), Map.class);


        container.start();
        return container;
    }
}
