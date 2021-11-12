package com.shankhadeepghoshal.chatapp.controller;

import com.shankhadeepghoshal.chatapp.configs.SqsMessageReceiver;
import com.shankhadeepghoshal.chatapp.configs.SqsMessageSender;
import com.shankhadeepghoshal.chatapp.model.MessageStruct;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Controller("/sqs")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ControllerSqs {
    SqsMessageSender messageSender;
    SqsMessageReceiver messageReceiver;

    @Post(consumes = MediaType.APPLICATION_JSON)
    public Single<Class<Void>> sendMessage(@Body MessageStruct message) {
        messageSender.sendMessage(message);
        messageReceiver.startReceivingMessage();
        return Single.just(Void.TYPE);
    }
}
