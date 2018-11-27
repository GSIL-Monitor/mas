package com.letv.mas.router.iptv.tvproxy.config;

//import org.ehcache.CacheManager;
//import org.ehcache.config.builders.CacheManagerBuilder;
//import org.ehcache.xml.XmlConfiguration;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.configuration.TypesafeConfigurator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import scala.concurrent.duration.Duration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
//import java.net.URL;

/**
 * Created by leeco on 18/11/25.
 */
@Configuration
public class CacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

    private ApplicationContext applicationContext;

    @Autowired
    CaffeineProperties caffeineProperties;

    @Data
    @Primary
    @Configuration
    @ConfigurationProperties(prefix = "caffeine.jcache")
    public static class CaffeineProperties {

        private List<Cache> caches = new ArrayList<>();

        @Data
        public static class Cache {
            /**
             * 缓存键名
             */
            private String name;

            /**
             * 访问后过期时间，单位毫秒
             */
            private long expireAfterAccess;

            /**
             * 写入后过期时间，单位毫秒
             */
            private long expireAfterWrite;

            /**
             * 写入后刷新时间，单位毫秒
             */
            private long refreshAfterWrite;

            /**
             * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
             */
            private long maximumSize;
        }
    }

    public CacheConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

//    @Bean(name = "cacheResolver")
//    @ConditionalOnProperty(value = {"bucket4j.enabled"}, havingValue = "true", matchIfMissing = false)
//    public CacheManager cacheManager() {
//        CacheManager cacheManager = null;
//        XmlConfiguration xmlConfig = null;
//        LOGGER.info("The ehcache manager is being initialized...");
//        try {
//            URL url = this.applicationContext.getClassLoader().getResource("ehcache-v3.xml");
//            xmlConfig = new XmlConfiguration(url);
//            cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
//            LOGGER.info("The ehcache manager is done!");
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//
//        return cacheManager;
//    }

    @Bean
    @ConditionalOnProperty(value = {"bucket4j.enabled"}, havingValue = "true", matchIfMissing = false)
    public javax.cache.CacheManager cacheManager() {
        javax.cache.CacheManager cacheManager = null;
        final CachingProvider cachingProvider = Caching.getCachingProvider(
        /*"com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider"*/);
        LOGGER.info("The caffeine manager is being initialized...");
        try {
            Config config = ConfigFactory.load();
            CaffeineConfiguration<String, Object> caffeineConfiguration = null;
            cacheManager = cachingProvider.getCacheManager();
            OptionalLong optionalLong = null;
            if (true) {
                if (null != caffeineProperties && null != caffeineProperties.getCaches()
                        && caffeineProperties.getCaches().size() > 0) {
                    for (CaffeineProperties.Cache cache: caffeineProperties.getCaches()) {
                        caffeineConfiguration = TypesafeConfigurator.defaults(config);

                        caffeineConfiguration.setMaximumSize(OptionalLong.of(cache.getMaximumSize()));

                        optionalLong = OptionalLong.of(cache.getExpireAfterAccess());
                        if (optionalLong.isPresent()) {
                            caffeineConfiguration.setExpireAfterAccess(
                                    OptionalLong.of(Duration.create(optionalLong.getAsLong(), TimeUnit.SECONDS).toNanos()));
                        }

                        optionalLong = OptionalLong.of(cache.getExpireAfterWrite());
                        if (optionalLong.isPresent()) {
                            caffeineConfiguration.setExpireAfterWrite(
                                    OptionalLong.of(Duration.create(optionalLong.getAsLong(), TimeUnit.SECONDS).toNanos()));
                        }

                        optionalLong = OptionalLong.of(cache.getRefreshAfterWrite());
                        if (optionalLong.isPresent()) {
                            caffeineConfiguration.setRefreshAfterWrite(
                                    OptionalLong.of(Duration.create(optionalLong.getAsLong(), TimeUnit.SECONDS).toNanos()));
                        }

                        if (StringUtils.isNotBlank(cache.getName())) {
                            cacheManager.createCache(cache.getName(), caffeineConfiguration);
                        }
                    }
                } else {
                    caffeineConfiguration = TypesafeConfigurator.defaults(config);
                    cacheManager.createCache("buckets", caffeineConfiguration);
                }
            }
            LOGGER.info("The caffeine manager is done[{}]!", null != cacheManager ? "OK" : "NG");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return cacheManager;
    }
}
