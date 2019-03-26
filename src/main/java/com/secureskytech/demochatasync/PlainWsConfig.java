package com.secureskytech.demochatasync;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

import lombok.AllArgsConstructor;

@Configuration
@EnableWebSocket
@AllArgsConstructor
public class PlainWsConfig implements WebSocketConfigurer {

    private final PlainWsDemoHandler plainWsDemoHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        final WebSocketHandler wrapped = new ExceptionWebSocketHandlerDecorator(
                new LoggingWebSocketHandlerDecorator(plainWsDemoHandler));
        registry.addHandler(wrapped, "/plain-ws-demo");
    }
}