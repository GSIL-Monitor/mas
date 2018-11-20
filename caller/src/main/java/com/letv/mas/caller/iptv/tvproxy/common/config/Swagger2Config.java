package com.letv.mas.caller.iptv.tvproxy.common.config;

import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.WildcardType;
import java.time.LocalDate;

import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.schema.AlternateTypeRules.newRule;

@Configuration
@ConditionalOnProperty(value = "spring.cloud.iptv.doc.enabled", havingValue= "true", matchIfMissing = false)
@EnableSwagger2
public class Swagger2Config {

    /*@Bean
    public Docket user_docket() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("user")
                .apiInfo(new ApiInfoBuilder().title("[超级影视］代理层接口WIKI").version("V2.0.0").build()).useDefaultResponseMessages(false).select()
                .apis(RequestHandlerSelectors.basePackage("com.letv.mas.caller.iptv.tvproxy")).build()
                .pathMapping("/");
    }*/

    @Autowired
    private TypeResolver typeResolver;

    @Bean
    public Docket proxyApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.letv.mas.caller.iptv.tvproxy"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .pathMapping("/")
                .directModelSubstitute(LocalDate.class, String.class)
                .genericModelSubstitutes(ResponseEntity.class)
                .alternateTypeRules(
                        newRule(typeResolver.resolve(DeferredResult.class,
                                typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                                typeResolver.resolve(WildcardType.class)))
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET,
                        newArrayList(new ResponseMessageBuilder()
                                .code(500)
                                .message("内部服务器错误!")
                                .responseModel(new ModelRef("Error"))
                                .build()))
                .enableUrlTemplating(true);
//                .tags(new Tag("大屏微服务代理层", "接口文档"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("大屏/微服务/超影代理层接口")
                .description("接口文档")
                .version("1.0.0")
                .contact(new Contact("maning", "http://omp.mas.letv.cn", "maning5@le.com"))
                .build();
    }

    @Bean
    UiConfiguration uiConfig() {
        return UiConfigurationBuilder.builder()
                .deepLinking(true)
                .displayOperationId(false)
                .defaultModelsExpandDepth(1)
                .defaultModelExpandDepth(1)
                .defaultModelRendering(ModelRendering.EXAMPLE)
                .displayRequestDuration(false)
                .docExpansion(DocExpansion.NONE)
                .filter(false)
                .maxDisplayedTags(null)
                .operationsSorter(OperationsSorter.ALPHA)
                .showExtensions(false)
                .tagsSorter(TagsSorter.ALPHA)
                .supportedSubmitMethods(UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS)
                .validatorUrl(null)
                .build();
    }

}
