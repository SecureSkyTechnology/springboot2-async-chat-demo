package com.secureskytech.demochatasync;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.secureskytech.demochatasync.service.Chatroom;
import com.secureskytech.demochatasync.service.ChatroomActor.IChatroomEmitter;
import com.secureskytech.demochatasync.service.ChatroomService;

@Controller
@RequestMapping("/sse-chatroom")
public class SseChatroomController {
    protected static final Logger LOG = LoggerFactory.getLogger(SseChatroomController.class);

    @Autowired
    ChatroomService chatroomService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "false") final boolean useLogDump, final Model m) {
        m.addAttribute("chatrooms", chatroomService.getChatrooms().values());
        if (useLogDump) {
            chatroomService.logdump();
        }
        return "sse-chatroom-list";
    }

    @PostMapping("create")
    public String createChatroom(@RequestParam final String name) {
        final Chatroom newChatroom = chatroomService.create(name);
        return "redirect:/sse-chatroom/" + newChatroom.getId() + "/";
    }

    @GetMapping("{chatroomId}/")
    public String chatroom(@PathVariable final String chatroomId, final Model m) {
        m.addAttribute("chatroom", chatroomService.getChatroom(chatroomId));
        return "sse-chatroom";
    }

    @GetMapping("{chatroomId}/enter")
    public SseEmitter enterChatroom(@PathVariable final String chatroomId) throws IOException {
        final String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 引数なしだと、デフォルト設定か数分でサーバ側でタイムアウトが発生する。
        // 在室確認用のping打ちを兼ねて、10分程度で明示的にタイムアウトを設定しておく。
        final SseEmitter sse = new SseEmitter(10 * 60 * 1000L);
        final IChatroomEmitter emitter = new SseChatroomEmitter(chatroomId, username, sse, (emitterId) -> {
            chatroomService.exitSilently(username, chatroomId, emitterId);
        });
        chatroomService.enter(username, chatroomId, emitter);
        LOG.info("user:{} entered to {} with emitterId:{}", username, chatroomId, emitter.getId());

        // 特殊なイベント名でemitterIdをクライアントに送信
        sse.send(SseEmitter.event().data(emitter.getId()).name("emitter-id"));

        return sse;
    }

    @PostMapping("{chatroomId}/post")
    @ResponseBody
    public String postMessage(@PathVariable final String chatroomId, final String message) throws IOException {
        final String username = SecurityContextHolder.getContext().getAuthentication().getName();
        chatroomService.postMessage(username, chatroomId, message);
        return "ok";
    }

    @GetMapping("{chatroomId}/exit")
    @ResponseBody
    public String exitChatroom(@PathVariable final String chatroomId, @RequestParam final String emitterId) {
        final String username = SecurityContextHolder.getContext().getAuthentication().getName();
        chatroomService.exit(username, chatroomId, emitterId);
        LOG.info("user:{} exit from {} with emitterId:{}", username, chatroomId, emitterId);
        return "exit success.";
    }

}