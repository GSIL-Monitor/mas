package com.letv.mas.router.iptv.tvproxy.config;

//import org.ehcache.CacheManager;
//import org.ehcache.config.builders.CacheManagerBuilder;
//import org.ehcache.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
//import java.net.URL;

/**
 * Created by leeco on 18/11/25.
 */
@Configuration
public class CacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

    private ApplicationContext applicationContext;

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

    @Bean(name = "cacheResolver")
    @ConditionalOnProperty(value = {"bucket4j.enabled"}, havingValue = "true", matchIfMissing = false)
    public javax.cache.CacheManager cacheManager() {
        javax.cache.CacheManager cacheManager = null;
        final CachingProvider cachingProvider = Caching.getCachingProvider(/*"com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider"*/);
        LOGGER.info("The caffeine manager is being initialized...");
        try {
            cacheManager = cachingProvider.getCacheManager();
            LOGGER.info("The caffeine manager is done[{}]!", null != cacheManager ? "OK" : "NG");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return cacheManager;
    }
}
