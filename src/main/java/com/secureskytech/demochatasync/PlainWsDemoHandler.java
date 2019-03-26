package com.secureskytech.demochatasync;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.util.UriComponentsBuilder;

import com.secureskytech.demochatasync.service.SpringManagedActorSystem;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@Component
public class PlainWsDemoHandler extends TextWebSocketHandler {
    protected static final Logger LOG = LoggerFactory.getLogger(PlainWsDemoHandler.class);

    @Autowired
    SpringManagedActorSystem managedActorSystem;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("websocket connection established.");
        LOG.info("WebSocketSession.getAcceptedProtocol()={}", session.getAcceptedProtocol());
        session.getAttributes().forEach((k, v) -> {
            LOG.info("WebSocketSession.getAttributes()=[{},{}]", k, v);

        });
        LOG.info("WebSocketSession.getBinaryMessageSizeLimit()={}", session.getBinaryMessageSizeLimit());
        session.getHandshakeHeaders().forEach((headerName, headerValues) -> {
            headerValues.forEach(headerValue -> {
                LOG.info("WebSocketSession.getHandshakeHeaders()=[{}, {}]", headerName, headerValue);

            });
        });
        LOG.info("WebSocketSession.getId()={}", session.getId());
        LOG.info("WebSocketSession.getLocalAddress()={}", session.getLocalAddress());
        LOG.info("WebSocketSession.getPrincipal()={}", session.getPrincipal());
        LOG.info("WebSocketSession.getRemoteAddress()={}", session.getRemoteAddress());
        LOG.info("WebSocketSession.getTextMessageSizeLimit()={}", session.getTextMessageSizeLimit());
        LOG.info("WebSocketSession.getUri()={}", session.getUri());
        LOG.info("WebSocketSession.isOpen()={}", session.isOpen());

        // これ、ここでwrapして正しいんだろうか・・・
        final WebSocketSession concurrentSession = new ConcurrentWebSocketSessionDecorator(session, 1024 * 200, 200);

        final ActorSystem actorSystem = managedActorSystem.getActorSystem();
        final URI uri = session.getUri();
        MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        // 細かいエラーは無視する。
        final long numOfCount = Long.parseLong(parameters.getFirst("numOfCount"));
        final long intervalSec = Long.parseLong(parameters.getFirst("intervalSec"));
        final int errval = Integer.parseInt(parameters.getFirst("errval"));
        actorSystem.actorOf(
                Props.create(AsyncCountUpTimerDemoActor.class, numOfCount, intervalSec, concurrentSession, errval));
    }

    static class Tick0 {
    }

    /**
     * N秒おきにカウントアップして {@link SseEmitter#send(Object)} し、指定カウントになったら終了する。 
     * コンストラクタオプションによっては途中で例外を発生させて、ビジネスロジック中での例外をエミュレートすることも可能。
     */
    static class AsyncCountUpTimerDemoActor extends AbstractActorWithTimers {
        final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
        int count = 0;
        final long numOfCount;
        final WebSocketSessionDecorator session;
        final int errval;

        /**
         * @param numOfCount カウントアップ最大値
         * @param intervalSec 何秒おきにカウントアップするかのスリープ秒数
         * @param sseEmitter
         * @param errval カウントがこの値に到達したら div by zero を発生させる。
         */
        AsyncCountUpTimerDemoActor(final long numOfCount, final long intervalSec,
                final WebSocketSessionDecorator session, final int errval) {
            getTimers().startPeriodicTimer("tick0", new Tick0(), Duration.ofSeconds(intervalSec));
            this.numOfCount = numOfCount;
            this.session = session;
            this.errval = errval;
        }

        @Override
        public void postStop() {
            if (session.isOpen()) {
                log.info("WebSocketSession is open, normal close.");
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.info("WebSocketSession close error: {}", e.getMessage());
                }
            }
            log.info("async count-down timer stopped, called WebSocketSession.close(CloseStatus.NORMAL).");
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder().match(Tick0.class, tick -> {
                count++;
                log.info("tick {}/{}", count, numOfCount);
                try {
                    // カウンタがerrvalに到達したら、div by zero を発生させる。
                    if (errval == count) {
                        count = count / (errval - count);
                    }
                } catch (ArithmeticException e) {
                    log.info("div/zero => stop");
                    session.close(CloseStatus.SERVER_ERROR);
                    getContext().stop(getSelf());
                }
                if (count >= numOfCount) {
                    log.info("tick count max => stop");
                    getContext().stop(getSelf());
                }
                try {
                    session.sendMessage(new TextMessage("count-" + count));
                } catch (IOException e) {
                    log.info("I/O error between peer connection : {} => stop", e.getMessage());
                    getContext().stop(getSelf());
                }
            }).build();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        LOG.info("websocket text message received.");
        LOG.info("WebSocketSession.getId()={}", session.getId());
        LOG.info("WebSocketSession.getUri()={}", session.getUri());
        LOG.info("WebSocketSession.isOpen()={}", session.isOpen());
        // クライアントからテキストメッセージが送信されたら、"thx:" prefixを付けてecho-backする。
        session.sendMessage(new TextMessage("thx:" + message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOG.info("websocket connection established.");
        LOG.info("WebSocketSession.getId()={}", session.getId());
        LOG.info("WebSocketSession.getUri()={}", session.getUri());
        LOG.info("WebSocketSession.isOpen()={}", session.isOpen());
    }
}
