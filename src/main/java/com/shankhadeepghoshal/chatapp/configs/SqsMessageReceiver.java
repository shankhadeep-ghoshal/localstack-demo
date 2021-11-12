package com.shankhadeepghoshal.chatapp.configs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Context
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = {@Inject, @Creator})
@Slf4j
public class SqsMessageReceiver {

    SqsAsyncClient sqsAsyncClient;
    ObjectMapper objectMapper;

    @Inject
    @Named("queueUrl")
    @NonFinal
    public String queueUrl;

    @SneakyThrows
    public void startReceivingMessage() {
        final var messageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl).build();
        final var response = sqsAsyncClient.receiveMessage(messageRequest).get();
        response.messages().forEach(this::handleMessageReceivedSuccessResponse);
        response.messages().forEach(this::deleteMessages);
    }

    private void handleMessageReceivedSuccessResponse(final Message message) {
        try {
            final var messageBody =
                    objectMapper.readValue(message.body(), new TypeReference<MessageStruct>() {});
            log.info(
                    String.join(
                            ", ",
                            "Out",
                            String.valueOf(System.currentTimeMillis()),
                            messageBody.toString()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void deleteMessages(Message message) {
        final var deleteRequest =
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
        sqsAsyncClient.deleteMessage(deleteRequest);
    }
}
