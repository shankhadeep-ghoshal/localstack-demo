package com.shankhadeepghoshal.chatapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shankhadeepghoshal.chatapp.configs.SqsMessageSender;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/*@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties(
								services = {ServiceName.SQS},
								ignoreDockerRunErrors = true,
								useSingleDockerContainer = true,
								environmentVariableProvider = EnvVars.class)*/
@MicronautTest
@TestInstance(Lifecycle.PER_CLASS)
public class MessagesTest {
    /*
    				public static class EnvVars implements IEnvironmentVariableProvider {

    								@Override
    								public Map<String, String> getEnvironmentVariables() {
    												return Map.of("USE_SSL", "0");
    								}
    				}
    */

    private final ObjectMapper objectMapper = new ObjectMapper();
    /*private SqsMessageSender messageSender;
    private String queueUrl;*/

    @Inject private SqsMessageSender messageSender;
    @Inject private SqsAsyncClient sqsClient;

    @Inject
    @Named("queue_url")
    private String queueUrl;

    /*
    @BeforeAll
    void setSqsQueue() {
    				*/
    /*
    								sqsClient = TestUtils.getClientSQSAsyncV2();
    								objectMapper = new ObjectMapper();
    								final var createQueueResponse = sqsClientFactory.createQueue(sqsClient);
    								queueUrl = new SqsQueueUrlFetcher().queueUrl(createQueueResponse);
    								messageSender = new SqsMessageSender(sqsClient, objectMapper, queueUrl);
    */
    /*

    				}
    */

    @RepeatedTest(5)
    void testMessageSend() {
        final var receiveCode =
                messageSender.sendMessage(new MessageStruct(1L, "1", "1", "1")).blockingGet();

        Assertions.assertEquals(200, receiveCode);
    }

    @RepeatedTest(5)
    void testMessageReceive() {
        messageSender.sendMessage(new MessageStruct(1L, "1", "1", "1")).blockingGet();
        ReceiveMessageRequest req = ReceiveMessageRequest.builder().queueUrl(queueUrl).build();
        final var response =
                Single.fromCompletionStage(sqsClient.receiveMessage(req)).blockingGet();

        Assertions.assertAll(
                "Asserting message size stuff... ",
                () -> Assertions.assertTrue(response.hasMessages()),
                () -> Assertions.assertTrue(response.messages().size() > 0));
        response.messages().stream()
                .findAny()
                .ifPresentOrElse(
                        message -> {
                            try {
                                final var mess =
                                        objectMapper.readValue(message.body(), MessageStruct.class);
                                Assertions.assertAll(
                                        "Asserting message properties... ",
                                        () -> Assertions.assertEquals("1", mess.body()),
                                        () -> Assertions.assertEquals("1", mess.roomId()),
                                        () -> Assertions.assertEquals("1", mess.senderId()),
                                        () -> Assertions.assertEquals(1L, mess.createdOn()));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                Assertions.fail(e);
                            }
                        },
                        () -> Assertions.fail("Message properties data incorrect"));
    }
}
