package com.shankhadeepghoshal.chatapp.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Context
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = {@Inject, @Creator})
@Slf4j
public class SqsMessageSender {

    SqsAsyncClient sqsAsyncClient;
    ObjectMapper objectMapper;

    @Inject
    @Named("queueUrl")
    @NonFinal
    public String queueUrl;

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
