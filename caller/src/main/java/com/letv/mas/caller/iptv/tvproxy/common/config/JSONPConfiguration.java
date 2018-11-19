package com.letv.mas.caller.iptv.tvproxy.common.config;

import com.alibaba.fastjson.support.spring.FastJsonpResponseBodyAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class JSONPConfiguration extends FastJsonpResponseBodyAdvice {

    public JSONPConfiguration() {
        super("callback", "jsonp");
    }
}