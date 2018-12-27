package com.secureskytech.demochatasync.service;

import java.util.UUID;

import lombok.Getter;
import lombok.ToString;

@ToString
public class Chatroom {
    @Getter
    private final String id;

    @Getter
    private final String name;

    public Chatroom(final String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }
}
