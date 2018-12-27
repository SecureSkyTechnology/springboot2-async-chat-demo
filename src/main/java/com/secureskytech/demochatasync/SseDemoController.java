package com.secureskytech.demochatasync;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.secureskytech.demochatasync.service.SpringManagedActorSystem;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

@Controller
@RequestMapping("/sse-demo")
public class SseDemoController {
    protected static final Logger LOG = LoggerFactory.getLogger(SseDemoController.class);

    @Autowired
    SpringManagedActorSystem managedActorSystem;

    @GetMapping
    public String index() {
        return "sse-demo";
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
        final SseEmitter sseEmitter;
        final int errval;

        /**
         * 
         * @param numOfCount カウントアップ最大値
         * @param intervalSec 何秒おきにカウントアップするかのスリープ秒数
         * @param sseEmitter
         * @param errval カウントがこの値に到達したら div by zero を発生させる。
         */
        AsyncCountUpTimerDemoActor(final long numOfCount, final long intervalSec, final SseEmitter sseEmitter,
                final int errval) {
            getTimers().startPeriodicTimer("tick0", new Tick0(), Duration.ofSeconds(intervalSec));
            this.numOfCount = numOfCount;
            this.sseEmitter = sseEmitter;
            this.errval = errval;
        }

        @Override
        public void postStop() {
            sseEmitter.complete();
            log.info("async count-down timer stopped, called sse-emitter.complete().");
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
                    // SSE専用の例外にラップすることで、他の例外ハンドラと処理を分ける。
                    sseEmitter.completeWithError(new SseDemoException(e));
                    getContext().stop(getSelf());
                }
                if (count >= numOfCount) {
                    log.info("tick count max => stop");
                    getContext().stop(getSelf());
                }
                try {
                    sseEmitter.send("count-" + count);
                    // @formatter:off
                    sseEmitter.send(
                            SseEmitter.event()
                                .data("count-with-other-field-" + count)
                                .name("count-up")
                                .id(UUID.randomUUID().toString()) // idフィールドの動作確認のため適当なランダム値を設定
                                .reconnectTime(3000)); // retryフィールドの動作確認のため適当なミリ秒を設定
                    // @formatter:on
                } catch (IOException e) {
                    log.info("I/O error between peer connection : {} => stop", e.getMessage());
                    getContext().stop(getSelf());
                } catch (IllegalStateException e) {
                    log.info("illegal state (connection closed or timeout expired) : {} => stop", e.getMessage());
                    getContext().stop(getSelf());
                }
            }).build();
        }
    }

    /**
     * 
     * @param numOfCount カウントアップ最大値
     * @param intervalSec 何秒おきにカウントアップするかのスリープ秒数
     * @param timeoutSec {@link SseEmitter#SseEmitter(Long)} にわたすタイムアウト秒数。
     *                    0の場合は {@link SseEmitter#SseEmitter()} を使う。
     * @param errval カウントがこの値に到達したら div by zero を発生させる。
     * @return
     * @throws IOException
     */
    @GetMapping("emit")
    public SseEmitter sse(@RequestParam(defaultValue = "1") long numOfCount,
            @RequestParam(defaultValue = "0") long intervalSec, @RequestParam(defaultValue = "0") long timeoutSec,
            @RequestParam(defaultValue = "999") int errval) throws IOException {
        LOG.info("Start get.");

        final ActorSystem actorSystem = managedActorSystem.getActorSystem();

        final SseEmitter emitter = (timeoutSec > 0) ? (new SseEmitter(timeoutSec * 1000)) : (new SseEmitter());
        emitter.onCompletion(() -> {
            LOG.info("SseEmitter.onCompletion()");
        });
        emitter.onTimeout(() -> {
            LOG.info("SseEmitter.onTimeout()");
        });
        emitter.onError((e) -> {
            LOG.warn("SseEmitter.onError()", e);
        });
        actorSystem.actorOf(Props.create(AsyncCountUpTimerDemoActor.class, numOfCount, intervalSec, emitter, errval));

        LOG.info("End get.");
        return emitter;
    }

}