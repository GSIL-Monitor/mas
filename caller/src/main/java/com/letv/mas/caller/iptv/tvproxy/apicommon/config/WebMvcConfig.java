package com.letv.mas.caller.iptv.tvproxy.apicommon.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.AuthorizedInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.CheckLoginInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.HttpParameterInterceptor;
import com.letv.mas.caller.iptv.tvproxy.apicommon.interceptor.HttpResponseInterceptor;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ComponentScan(basePackages = {"com.letv.mas.caller.iptv.tvproxy.*"}, excludeFilters = {@ComponentScan.Filter(Configuration.class)})
@Configuration
@EnableWebMvc
public class WebMvcConfig extends WebMvcConfigurerAdapter {
    private static final Logger logger = Logger.getLogger(WebMvcConfig.class);

    @Bean
    AuthorizedInterceptor getAuthorizedInterceptor(){
        return new AuthorizedInterceptor();
    }
    @Bean
    CheckLoginInterceptor getCheckLoginInterceptor(){
        return new CheckLoginInterceptor();
    }
    @Bean
    HttpParameterInterceptor getHttpParameterInterceptor(){
        return new HttpParameterInterceptor();
    }
    @Bean
    HttpResponseInterceptor getHttpResponseInterceptor(){
        return new HttpResponseInterceptor();
    }


    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        Map<String, MediaType> fastMediaTypes = new HashMap<>();
        fastMediaTypes.put("json",MediaType.APPLICATION_JSON_UTF8);
        fastMediaTypes.put("jsonp",new MediaType("application","javascript"));
        configurer.mediaTypes(fastMediaTypes);
        configurer.useJaf(false).favorPathExtension(true).favorParameter(false)
                .ignoreAcceptHeader(true).defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();    //添加fastJson的配置信息
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(SerializerFeature.DisableCircularReferenceDetect);
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        fastMediaTypes.add(FastJsonHttpMessageConverter.APPLICATION_JAVASCRIPT);
        fastJsonHttpMessageConverter.setSupportedMediaTypes(fastMediaTypes);
        fastJsonConfig.setCharset(Charset.forName("UTF-8"));
        fastJsonHttpMessageConverter.setFastJsonConfig(fastJsonConfig);
        HttpMessageConverter<?> converter = fastJsonHttpMessageConverter;
        converters.add(converter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getAuthorizedInterceptor())    //指定拦截器类
                .addPathPatterns("/iptv/api/**");
        registry.addInterceptor(getCheckLoginInterceptor())
                .addPathPatterns("/iptv/api/**");
        registry.addInterceptor(getHttpParameterInterceptor())
                .addPathPatterns("/iptv/api/**");
        registry.addInterceptor(getHttpResponseInterceptor())
                .addPathPatterns("/iptv/api/**");
        super.addInterceptors(registry);
    }
}
