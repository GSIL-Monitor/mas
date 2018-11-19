package com.letv.mas.caller.iptv.tvproxy.apicommon.config;

import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.AuthorizedInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.CheckLoginInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.HttpParameterInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.HttpResponseInterceptor;
import com.letv.mas.caller.iptv.tvproxy.common.config.WebMvcConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;


@Configuration
public class AWebMvcConfig extends WebMvcConfig {
    private static final Logger logger = Logger.getLogger(AWebMvcConfig.class);

    @Bean
    AuthorizedInterceptor authorizedInterceptor(){
        return new AuthorizedInterceptor();
    }
    @Bean
    CheckLoginInterceptor checkLoginInterceptor(){
        return new CheckLoginInterceptor();
    }
    @Bean
    HttpParameterInterceptor httpParameterInterceptor(){
        return new HttpParameterInterceptor();
    }
    @Bean
    HttpResponseInterceptor httpResponseInterceptor(){
        return new HttpResponseInterceptor();
    }

    @Autowired
    AuthorizedInterceptor authorizedInterceptor;

    @Autowired
    CheckLoginInterceptor checkLoginInterceptor;

    @Autowired
    HttpParameterInterceptor httpParameterInterceptor;

    @Autowired
    HttpResponseInterceptor httpResponseInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizedInterceptor)    //指定拦截器类
                .addPathPatterns("/**");
        registry.addInterceptor(checkLoginInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(httpParameterInterceptor)
                .addPathPatterns("/**");
        registry.addInterceptor(httpResponseInterceptor)
                .addPathPatterns("/**");
        super.addInterceptors(registry);
    }
}
