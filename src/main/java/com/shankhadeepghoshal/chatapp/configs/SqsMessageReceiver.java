package com.shankhadeepghoshal.chatapp.configs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.scheduling.annotation.Scheduled;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Singleton
@Introspected
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.UncommentedEmptyMethodBody",
    "PMD.AvoidDuplicateLiterals",
    "PMD.DoNotUseThreads"
})
public class SqsMessageReceiver {

    transient SqsAsyncClient sqsAsyncClient;
    transient ObjectMapper objectMapper;
    transient ThreadPoolExecutor customThreadPool;
    transient String queueUrl;

    @Inject
    @Creator
    public SqsMessageReceiver(
            SqsAsyncClient sqsAsyncClient,
            ObjectMapper objectMapper,
            @Named("forConsoleOut") ThreadPoolExecutor customThreadPool,
            @Named("queue_url") String queueUrl) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.customThreadPool = customThreadPool;
        this.queueUrl = queueUrl;
    }

    @SneakyThrows
    @Scheduled(fixedRate = "1s", initialDelay = "0s")
    public void startReceivingMessage() {
        final var messageRequest =
                Single.fromCallable(
                                () -> ReceiveMessageRequest.builder().queueUrl(queueUrl).build())
                        .subscribeOn(Schedulers.from(customThreadPool, true, true))
                        .observeOn(Schedulers.from(customThreadPool, true, true));
        readMessage(messageRequest);
    }

    @PreDestroy
    void shutdownBlockingScheduler() {
        customThreadPool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                customThreadPool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            customThreadPool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void readMessage(Single<ReceiveMessageRequest> messageRequest) {
        messageRequest
                .subscribeOn(Schedulers.from(customThreadPool, true, true))
                .map(sqsAsyncClient::receiveMessage)
                .doOnEvent(
                        (stuff, thr) ->
                                log.info(
                                        "Executing Receive Event {}",
                                        Thread.currentThread().getName()))
                .flatMap(Single::fromCompletionStage)
                .map(ReceiveMessageResponse::messages)
                .observeOn(Schedulers.from(customThreadPool, true, true))
                //                .doOnSuccess(messages -> messages.forEach(this::deleteMessages))
                .subscribe(
                        new SingleObserver<>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                log.info(
                                        "Receive Subscribed to thread {}",
                                        Thread.currentThread().getName());
                            }

                            @Override
                            public void onSuccess(@NonNull List<Message> messages) {
                                messages.forEach(
                                        SqsMessageReceiver.this
                                                ::handleMessageReceivedSuccessResponse);
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                log.error("Error receiving messages {}", e.getMessage());
                                log.info("Current thread {}", Thread.currentThread().getName());
                            }
                        });
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
            log.info("Receive Current thread {}", Thread.currentThread().getName());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void deleteMessages(Message message) {
        final var deleteMessageRequest =
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();

        Single.fromCompletionStage(sqsAsyncClient.deleteMessage(deleteMessageRequest))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(customThreadPool, true, true))
                .subscribe(
                        new SingleObserver<>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                log.info(
                                        "Delete Subscribed to thread {}",
                                        Thread.currentThread().getName());
                            }

                            @Override
                            public void onSuccess(
                                    @NonNull DeleteMessageResponse deleteMessageResponse) {
                                log.info(
                                        "Deleted message HTTP status {}",
                                        deleteMessageResponse.sdkHttpResponse().statusCode());
                                log.info(
                                        "Delete Current thread {}",
                                        Thread.currentThread().getName());
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                log.error("Delete message error {}", e.getMessage());
                                log.info(
                                        "Delete Current thread {}",
                                        Thread.currentThread().getName());
                            }
                        });
    }
}
