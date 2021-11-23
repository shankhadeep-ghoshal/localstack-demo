package com.shankhadeepghoshal.chatapp.configs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Named;
import java.net.URI;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

@Factory
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SqsClientFactory {

    transient String test = "test";
    transient String sqsEndpoint;
    transient String messagesQueue;

    @Creator
    public SqsClientFactory(
            final @Property(name = "sqs.url", defaultValue = "http://127.0.0.1:4566") String
                            sqsEndpoint,
            final @Property(name = "sqs.message-queue-name", defaultValue = "message_queue") String
                            messagesQueue) {
        this.sqsEndpoint = sqsEndpoint;
        this.messagesQueue = messagesQueue;
    }

    @SneakyThrows
    @Bean
    @Primary
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                new AwsCredentials() {
                                    @Override
                                    public String accessKeyId() {
                                        return test;
                                    }

                                    @Override
                                    public String secretAccessKey() {
                                        return test;
                                    }
                                }))
                .build();
    }

    @Bean
    @Named("created-response")
    public CreateQueueResponse createQueue(final SqsAsyncClient client) {
        final var createQueueRequest =
                CreateQueueRequest.builder().queueName(messagesQueue).build();
        return Single.fromCompletionStage(client.createQueue(createQueueRequest))
                .doOnSuccess(this::logQueueCreationMessageSuccess)
                .doOnError(this::logQueueCreationMessageError)
                .blockingGet();
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void logQueueCreationMessageSuccess(final CreateQueueResponse response) {
        log.info("Created queue Status: {}", response.sdkHttpResponse().statusCode());
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void logQueueCreationMessageError(final Throwable exception) {
        log.error("Created queue Status: {}", exception.getMessage());
    }
}
