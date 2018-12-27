package com.secureskytech.demochatasync;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.secureskytech.demochatasync.service.ChatroomActor.IChatroomEmitter;

public class SseChatroomEmitter implements IChatroomEmitter {
    protected static final Logger LOG = LoggerFactory.getLogger(SseChatroomEmitter.class);

    private final String id;
    private final String boundChannelIdRef;
    private final String boundUsernameRef;
    private final SseEmitter sse;
    private final Consumer<String> onSendError;

    /**
     * 
     * @param boundChannelIdRef このemitterに紐付いたchannelId
     * @param boundUsernameRef このemitterに紐付いたユーザ名
     * @param sse 使用する {@link SseEmitter}
     * @param onSendError {@link SseEmitter#send(Object)} で例外が発生した時に何か処理させたいときのコールバック。
     *                     引数として emitterId を渡す。
     */
    public SseChatroomEmitter(final String boundChannelIdRef, final String boundUsernameRef, final SseEmitter sse,
            final Consumer<String> onSendError) {
        this.id = UUID.randomUUID().toString();
        this.boundChannelIdRef = boundChannelIdRef;
        this.boundUsernameRef = boundUsernameRef;
        this.sse = sse;
        this.onSendError = onSendError;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void emit(String username, String message) {
        try {
            sse.send(username + "さん: " + message);
        } catch (IOException e) {
            LOG.info("I/O error between peer connection : {} => stop <> channelId={}, username={}", e.getMessage(),
                    boundChannelIdRef, boundUsernameRef);
            onSendError.accept(id);
        } catch (IllegalStateException e) {
            LOG.info("illegal state (connection closed or timeout expired) : {} => stop <> channelId={}, username={}",
                    e.getMessage(), boundChannelIdRef, boundUsernameRef);
            onSendError.accept(id);
        }
    }

    @Override
    public String toString() {
        return "ChatroomSseEmitter [id=" + id + ", boundChannelIdRef=" + boundChannelIdRef + ", boundUsernameRef="
                + boundUsernameRef + ", sse=" + sse + "]";
    }
}
