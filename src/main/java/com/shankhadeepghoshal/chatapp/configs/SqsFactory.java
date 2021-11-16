package com.shankhadeepghoshal.chatapp.configs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
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
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@Factory
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SqsFactory {

    private static final String TEST = "test";

    transient String sqsEndpoint;
    transient String messagesQueue;

    @Creator
    public SqsFactory(
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
        final var client =
                SqsAsyncClient.builder()
                        .endpointOverride(URI.create(sqsEndpoint))
                        .region(Region.US_EAST_1)
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        new AwsCredentials() {
                                            @Override
                                            public String accessKeyId() {
                                                return TEST;
                                            }

                                            @Override
                                            public String secretAccessKey() {
                                                return TEST;
                                            }
                                        }))
                        .build();
        createQueue(client);
        return client;
    }

    public void createQueue(final SqsAsyncClient client) {
        final var createQueueRequest =
                CreateQueueRequest.builder().queueName(messagesQueue).build();
        Single.fromCompletionStage(client.createQueue(createQueueRequest))
                .doOnSuccess(this::logQueueCreationMessageSuccess)
                .doOnError(this::logQueueCreationMessageError)
                .blockingSubscribe();
    }

    @SneakyThrows
    @Bean
    @Named("queueUrl")
    @Requires(beans = SqsAsyncClient.class)
    public String queueUrl(final SqsAsyncClient sqsAsyncClient) {
        try {
            final var queueCreateReq =
                    sqsAsyncClient.getQueueUrl(
                            GetQueueUrlRequest.builder().queueName(messagesQueue).build());
            return Single.fromCompletionStage(queueCreateReq)
                    .doOnSuccess(
                            response ->
                                    log.info(
                                            "Queue Url status {}",
                                            response.sdkHttpResponse().statusCode()))
                    .doOnError(error -> log.error("Queue Url status error {}", error.getMessage()))
                    .map(GetQueueUrlResponse::queueUrl)
                    .blockingGet();
        } catch (QueueDoesNotExistException e) {
            e.printStackTrace();
            return "";
        }
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
