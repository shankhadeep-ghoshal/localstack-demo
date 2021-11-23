package com.shankhadeepghoshal.chatapp.configs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

@Factory
public class SqsQueueUrlFetcher {

    @Bean
    @Named("queue_url")
    public String queueUrl(@Named("created-response") final CreateQueueResponse response) {
        return response.queueUrl();
    }
}
