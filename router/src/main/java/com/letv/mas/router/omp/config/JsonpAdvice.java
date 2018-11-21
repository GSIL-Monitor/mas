package com.letv.mas.router.omp.config;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractJsonpResponseBodyAdvice;

/**
 * Created by zhangshuai6 on 2018/11/13.
 */
@RestControllerAdvice(basePackages = "com.letv.mas.router.omp.controller")
public class JsonpAdvice extends AbstractJsonpResponseBodyAdvice {

    public JsonpAdvice() {
        super("callback", "jsonp");
    }
}

