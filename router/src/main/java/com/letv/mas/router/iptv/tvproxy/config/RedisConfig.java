package com.letv.mas.router.iptv.tvproxy.config;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.DefaultLettucePool;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePool;
import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by leeco on 18/11/14.
 * refer: https://lettuce.io/core/release/reference/index.html
 */
@Configuration
@ConditionalOnProperty(value = {"spring.data.redis.repositories.enabled"}, havingValue = "true", matchIfMissing = false)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisConfig {

    @Autowired
    RedisProperties redisProperties;

    @Data
    @Primary
    @Configuration
    @ConfigurationProperties(prefix = "spring.redis")
    public static class RedisProperties {
        private Integer database;
        private String host;
        private String password;
        private Long timeout;
        private Pool pool;
        private Cluster cluster;

        @Data
        public static class Pool {
            private Integer maxIdle;
            private Integer minIdle;
            private Integer maxActive;
            private Integer maxWait;
            // 连接耗尽时是否阻塞, false报异常,true阻塞直到超时, 默认true
            private boolean isBlockWhenExhausted = true;
            // 在borrow一个实例时，是否提前进行alidate操作；如果为true，则得到的实例均是可用的，默认false
            private boolean isTestOnBorrow = false;
            // 调用returnObject方法时，是否进行有效检查，默认false
            private boolean isTestOnReturn = false;
            // 在空闲时检查有效性, 默认false
            private boolean isTestWhileIdle = false;
            // 运行一次空闲连接回收器的间隔；
            private Long timeBetweenEvictionRunsMillis;
            // 池中的连接空闲后被回收的间隔，这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义；
            private Long minEvictableIdleTimeMillis;
        }

        @Data
        public static class Cluster {
            private Nodes master;
            private Nodes slave;

            @Data
            public static class Nodes {
                private String host;
                private String password;
                private Long timeout;
            }
        }
    }

    @Bean
    public GenericObjectPoolConfig getRedisConfig() {
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(redisProperties.getPool().maxIdle);
        genericObjectPoolConfig.setMaxTotal(redisProperties.getPool().maxActive);
        genericObjectPoolConfig.setMinIdle(redisProperties.getPool().minIdle);
        genericObjectPoolConfig.setBlockWhenExhausted(redisProperties.getPool().isBlockWhenExhausted);
        genericObjectPoolConfig.setMaxWaitMillis(redisProperties.getPool().maxWait);
        genericObjectPoolConfig.setTestOnBorrow(redisProperties.getPool().isTestOnBorrow);
        genericObjectPoolConfig.setTestOnReturn(redisProperties.getPool().isTestOnReturn);
        genericObjectPoolConfig.setTestWhileIdle(redisProperties.getPool().isTestWhileIdle);
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(redisProperties.getPool().timeBetweenEvictionRunsMillis);
        genericObjectPoolConfig.setMinEvictableIdleTimeMillis(redisProperties.getPool().minEvictableIdleTimeMillis);
        return genericObjectPoolConfig;
    }

    @Bean
    public DefaultLettucePool getDefaultLettucePool(GenericObjectPoolConfig poolConfig) {
        String[] node = redisProperties.getHost().split(":");
        DefaultLettucePool defaultLettucePool = new DefaultLettucePool(node[0], Integer.parseInt(node[1]), poolConfig);
        defaultLettucePool.setPassword(redisProperties.getPassword());
        defaultLettucePool.afterPropertiesSet();
        return defaultLettucePool;
    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(LettucePool pool) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(pool);
        factory.setValidateConnection(true);
        factory.setDatabase(redisProperties.getDatabase());
        factory.setTimeout(redisProperties.getTimeout());
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean(name = "redisCacheTemplate")
    public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        // 对key采用String的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
//        template.setEnableTransactionSupport(true);
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 对value采用String的序列化方式
        template.setValueSerializer(new StringRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);

        return template;
    }

//    @Bean
    public RedisCacheManager getRedisCacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }

    @RefreshScope
    @Bean(name = "ledisMasterClusterConnection")
    @ConditionalOnBean(RedisConfig.RedisProperties.class)
    public StatefulRedisClusterConnection ledisMasterClusterConnection() {
        return this.getConnection(redisProperties.getCluster().getMaster(), ReadFrom.MASTER);
    }

    @RefreshScope
    @Bean(name = "ledisSlaveClusterConnection")
    @ConditionalOnBean(RedisConfig.RedisProperties.class)
    public StatefulRedisClusterConnection ledisSlaveClusterConnection() {
        return this.getConnection(redisProperties.getCluster().getSlave(), ReadFrom.SLAVE);
    }

    private StatefulRedisClusterConnection getConnection(RedisProperties.Cluster.Nodes nodes, ReadFrom readFrom) {
        StatefulRedisClusterConnection<String, String> connection = null;
        if (null != nodes) {
            String strNodes = nodes.host;
            String strPwd = nodes.password;
            Long timeout = nodes.timeout;
            String[] aryNodes = strNodes.split(",");
            List<RedisURI> listRedisURIs = new ArrayList<RedisURI>();
            String[] nodeInfo = null;
            RedisURI redisURI = null;

            if (StringUtils.isBlank(strPwd)) {
                strPwd = "";
            }
            for (String node : aryNodes) {
                nodeInfo = node.split(":");
                // "redis://password@host:port/timeout"
                redisURI = RedisURI.Builder.redis(nodeInfo[0], Integer.parseInt(nodeInfo[1]))
                        .withPassword(strPwd).withDatabase(1).withTimeout(timeout, TimeUnit.MILLISECONDS).build();
                listRedisURIs.add(redisURI);
            }
            RedisClusterClient clusterClient = RedisClusterClient.create(listRedisURIs);
            try {
                connection = clusterClient.connect();
            } catch (Exception e) {

            }
            if (null != connection) {
                connection.setReadFrom(readFrom);
            }
        }
        return connection;
    }
}
