package com.shankhadeepghoshal.chatapp.configs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Named;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@Factory
@Introspected
@FieldDefaults(level = AccessLevel.PUBLIC)
public class SqsFactory {

    private static final String TEST = "test";

    @Property(name = "sqs.url", defaultValue = "http://127.0.0.1:4566")
    String sqsEndpoint;

    @Property(name = "sqs.message-queue-name", defaultValue = "message_queue")
    String messagesQueue;

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

    public void createQueue(final SqsAsyncClient client)
            throws ExecutionException, InterruptedException {
        final var createQueueRequest =
                CreateQueueRequest.builder().queueName(messagesQueue).build();
        client.createQueue(createQueueRequest).get();
    }

    @SneakyThrows
    @Bean
    @Named("queueUrl")
    public String queueUrl(final SqsAsyncClient sqsAsyncClient) {
        try {
            return sqsAsyncClient
                    .getQueueUrl(GetQueueUrlRequest.builder().queueName(messagesQueue).build())
                    .get()
                    .queueUrl();
        } catch (QueueDoesNotExistException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getSqsEndpoint() {
        return sqsEndpoint;
    }

    public void setSqsEndpoint(String sqsEndpoint) {
        this.sqsEndpoint = sqsEndpoint;
    }

    public String getMessagesQueue() {
        return messagesQueue;
    }

    public void setMessagesQueue(String messagesQueue) {
        this.messagesQueue = messagesQueue;
    }
}
