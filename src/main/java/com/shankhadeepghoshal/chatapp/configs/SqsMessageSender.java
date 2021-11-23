package com.shankhadeepghoshal.chatapp.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Singleton
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SqsMessageSender {

    transient SqsAsyncClient sqsAsyncClient;
    transient ObjectMapper objectMapper;
    transient String queueUrl;

    @Inject
    @Creator
    public SqsMessageSender(
            SqsAsyncClient sqsAsyncClient,
            ObjectMapper objectMapper,
            @Named("queue_url") String queueUrl) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @SneakyThrows
    public Single<Integer> sendMessage(final MessageStruct messageBody) {
        log.info("Send message to queue {}", this.queueUrl);
        return sendMessageCompletableFuture(messageBody).subscribeOn(Schedulers.io());
    }

    private Single<Integer> sendMessageCompletableFuture(MessageStruct messageBody) {
        final var senderRequestSingle =
                Single.fromCallable(
                        () ->
                                SendMessageRequest.builder()
                                        .queueUrl(queueUrl)
                                        .messageBody(objectMapper.writeValueAsString(messageBody))
                                        .delaySeconds(0)
                                        .build());
        return senderRequestSingle
                .map(sqsAsyncClient::sendMessage)
                .flatMap(Single::fromCompletionStage)
                .map(response -> response.sdkHttpResponse().statusCode());
    }
}
