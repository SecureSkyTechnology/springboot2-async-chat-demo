package com.secureskytech.demochatasync.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.Data;

public class ChatroomActor extends AbstractActor {

    public static interface IChatroomEmitter {
        String getId();

        void emit(final String username, final String message);
    }

    @Data
    public static class EnterMessage {
        private final String username;
        private final String chatroomId;
        private final IChatroomEmitter emitter;
    }

    @Data
    public static class ExitMessage {
        private final String username;
        private final String chatroomId;
        private final String emitterId;
    }

    @Data
    public static class ExitSilentlyMessage {
        private final String username;
        private final String chatroomId;
        private final String emitterId;
    }

    @Data
    public static class PostMessage {
        private final String username;
        private final String chatroomId;
        private final String message;
    }

    public static class LogDumpMessage {
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, Map<String, Map<String, IChatroomEmitter>>> mapOfChatroomToUserToEmitter = new HashMap<>();

    private Map<String, IChatroomEmitter> getOrDefaultEmitterMap(final String chatroomId, final String username) {
        final Map<String, Map<String, IChatroomEmitter>> mapOfUserToEmitter = mapOfChatroomToUserToEmitter
                .getOrDefault(chatroomId, new HashMap<>());
        return mapOfUserToEmitter.getOrDefault(username, new HashMap<>());
    }

    private void saveEmitterMap(final String chatroomId, final String username,
            Map<String, IChatroomEmitter> emitterMap) {
        final Map<String, Map<String, IChatroomEmitter>> mapOfUserToEmitter = mapOfChatroomToUserToEmitter
                .getOrDefault(chatroomId, new HashMap<>());
        mapOfUserToEmitter.put(username, emitterMap);
        mapOfChatroomToUserToEmitter.put(chatroomId, mapOfUserToEmitter);
    }

    private void postMessage(final String chatroomId, final String username, final String message) {
        final Map<String, Map<String, IChatroomEmitter>> mapOfUserToEmitter = mapOfChatroomToUserToEmitter
                .getOrDefault(chatroomId, new HashMap<>());
        for (final Map<String, IChatroomEmitter> emitters : mapOfUserToEmitter.values()) {
            for (final IChatroomEmitter emitter : emitters.values()) {
                emitter.emit(username, message);
            }
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(EnterMessage.class, m -> {

            final Map<String, IChatroomEmitter> emitters = getOrDefaultEmitterMap(m.getChatroomId(), m.getUsername());
            emitters.put(m.getEmitter().getId(), m.getEmitter());
            saveEmitterMap(m.getChatroomId(), m.getUsername(), emitters);
            postMessage(m.getChatroomId(), m.getUsername(), "入室しました");

        }).match(PostMessage.class, m -> {

            postMessage(m.getChatroomId(), m.getUsername(), m.getMessage());

        }).match(ExitMessage.class, m -> {

            final Map<String, IChatroomEmitter> emitters = getOrDefaultEmitterMap(m.getChatroomId(), m.getUsername());
            emitters.remove(m.getEmitterId());
            saveEmitterMap(m.getChatroomId(), m.getUsername(), emitters);
            postMessage(m.getChatroomId(), m.getUsername(), "退室しました");

        }).match(ExitSilentlyMessage.class, m -> {

            final Map<String, IChatroomEmitter> emitters = getOrDefaultEmitterMap(m.getChatroomId(), m.getUsername());
            emitters.remove(m.getEmitterId());
            saveEmitterMap(m.getChatroomId(), m.getUsername(), emitters);

        }).match(LogDumpMessage.class, m -> {

            for (Entry<String, Map<String, Map<String, IChatroomEmitter>>> entry1 : mapOfChatroomToUserToEmitter
                    .entrySet()) {
                final String chatroomId = entry1.getKey();
                final Map<String, Map<String, IChatroomEmitter>> mapOfUserToEmitter = entry1.getValue();
                for (Entry<String, Map<String, IChatroomEmitter>> entry2 : mapOfUserToEmitter.entrySet()) {
                    final String username = entry2.getKey();
                    final Map<String, IChatroomEmitter> emitters = entry2.getValue();
                    for (final String emitterId : emitters.keySet()) {
                        log.info("dump : chatroomId={}, username={}, emitterId={}", chatroomId, username, emitterId);
                    }
                }
            }

        }).build();
    }

}
