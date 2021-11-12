package com.shankhadeepghoshal.chatapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageStruct(
        @JsonProperty("createdOn") Long createdOn,
        @JsonProperty("senderId") String senderId,
        @JsonProperty("roomId") String roomId,
        @JsonProperty("body") String body) {

    @Override
    public String toString() {
        return String.join(", ", String.valueOf(createdOn), senderId, roomId, body);
    }
}
