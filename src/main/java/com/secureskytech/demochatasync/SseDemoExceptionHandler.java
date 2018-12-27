package com.secureskytech.demochatasync;

import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @see https://qiita.com/kazuki43zoo/items/53b79fe91c41cc5c2e59
 * 上記記事の「ExceptionResolverの仕組みと連携してエラーを通知」を参考に例外ハンドラを外出にしてみた。
 * Controller とは分けたので <code>ControllerAdvice</code> アノテーションを設定した。
 * <code>ResponseBody</code> アノテーションを指定しないとviewNameとして戻り値が解釈されてしまうので注意。
 */
@ControllerAdvice
public class SseDemoExceptionHandler {

    @ExceptionHandler
    @ResponseBody
    public String handleException(SseDemoException e) {
        return SseEmitter.event().data("error !!").build().stream().map(d -> d.getData().toString())
                .collect(Collectors.joining());
    }
}
