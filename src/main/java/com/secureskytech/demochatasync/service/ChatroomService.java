package com.secureskytech.demochatasync.service;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secureskytech.demochatasync.service.ChatroomActor.EnterMessage;
import com.secureskytech.demochatasync.service.ChatroomActor.ExitMessage;
import com.secureskytech.demochatasync.service.ChatroomActor.ExitSilentlyMessage;
import com.secureskytech.demochatasync.service.ChatroomActor.IChatroomEmitter;
import com.secureskytech.demochatasync.service.ChatroomActor.LogDumpMessage;
import com.secureskytech.demochatasync.service.ChatroomActor.PostMessage;

import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.Getter;

@Service
public class ChatroomService {
    protected static final Logger LOG = LoggerFactory.getLogger(ChatroomService.class);

    @Autowired
    SpringManagedActorSystem managedActorSystem;

    @Getter
    private final Map<String, Chatroom> chatrooms = new LinkedHashMap<>();

    private ActorRef chatroomActorRef;

    public Chatroom create(final String name) {
        final Chatroom newChatroom = new Chatroom(name);
        chatrooms.put(newChatroom.getId(), newChatroom);
        LOG.info("new chatroom created : {}", newChatroom);
        return newChatroom;
    }

    public Chatroom getChatroom(final String chatroomId) {
        return chatrooms.get(chatroomId);
    }

    public void enter(final String username, final String chatroomId, final IChatroomEmitter emitter) {
        final EnterMessage m = new EnterMessage(username, chatroomId, emitter);
        chatroomActorRef.tell(m, ActorRef.noSender());
        LOG.info("enter: {}", m);
    }

    public void exit(final String username, final String chatroomId, final String emitterId) {
        final ExitMessage m = new ExitMessage(username, chatroomId, emitterId);
        chatroomActorRef.tell(m, ActorRef.noSender());
        LOG.info("exit: {}", m);
    }

    public void exitSilently(final String username, final String chatroomId, final String emitterId) {
        final ExitSilentlyMessage m = new ExitSilentlyMessage(username, chatroomId, emitterId);
        chatroomActorRef.tell(m, ActorRef.noSender());
        LOG.info("exit: {}", m);
    }

    public void postMessage(final String username, final String chatroomId, final String message) {
        final PostMessage m = new PostMessage(username, chatroomId, message);
        chatroomActorRef.tell(m, ActorRef.noSender());
        LOG.info("post: {}", m);
    }

    public void logdump() {
        final LogDumpMessage m = new LogDumpMessage();
        chatroomActorRef.tell(m, ActorRef.noSender());
    }

    @PostConstruct
    public void populateChatroomService() {
        chatroomActorRef = managedActorSystem.getActorSystem().actorOf(Props.create(ChatroomActor.class));
        LOG.info("chatroom service populated.");
    }

    @PreDestroy
    public void destroyChatroomService() {
        LOG.info("chatroom service destroyed.");
    }
}
