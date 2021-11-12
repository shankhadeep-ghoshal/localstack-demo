package com.shankhadeepghoshal.chatapp.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
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
    public void sendMessage(final MessageStruct messageBody) {
        sqsAsyncClient
                .sendMessage(
                        SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .messageBody(objectMapper.writeValueAsString(messageBody))
                                .delaySeconds(0)
                                .build())
                .get();
    }
}
