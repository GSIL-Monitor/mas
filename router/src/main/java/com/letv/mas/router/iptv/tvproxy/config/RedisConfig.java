package com.letv.mas.router.iptv.tvproxy.config;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.letv.mas.router.iptv.tvproxy.plugin.redis.RedisProperties;
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
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
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

    @Bean
    public GenericObjectPoolConfig getRedisConfig() {
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(redisProperties.getPool().getMaxIdle());
        genericObjectPoolConfig.setMaxTotal(redisProperties.getPool().getMaxActive());
        genericObjectPoolConfig.setMinIdle(redisProperties.getPool().getMinIdle());
        genericObjectPoolConfig.setBlockWhenExhausted(redisProperties.getPool().isBlockWhenExhausted());
        genericObjectPoolConfig.setMaxWaitMillis(redisProperties.getPool().getMaxWait());
        genericObjectPoolConfig.setTestOnBorrow(redisProperties.getPool().isTestOnBorrow());
        genericObjectPoolConfig.setTestOnReturn(redisProperties.getPool().isTestOnReturn());
        genericObjectPoolConfig.setTestWhileIdle(redisProperties.getPool().isTestWhileIdle());
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(redisProperties.getPool().getTimeBetweenEvictionRunsMillis());
        genericObjectPoolConfig.setMinEvictableIdleTimeMillis(redisProperties.getPool().getMinEvictableIdleTimeMillis());
        return genericObjectPoolConfig;
    }

    @Bean
    @Primary
    public DefaultLettucePool getDefaultLettucePool(GenericObjectPoolConfig poolConfig) {
        String[] node = null;
        DefaultLettucePool defaultLettucePool = null;
        if (null == redisProperties.getSentinel()) {
            node = redisProperties.getHost().split(":");
            defaultLettucePool = new DefaultLettucePool(node[0], Integer.parseInt(node[1]), poolConfig);
            defaultLettucePool.setPassword(redisProperties.getPassword());
            defaultLettucePool.afterPropertiesSet();
        } else {
            RedisSentinelConfiguration redisSentinelConfiguration = null;
            List<RedisNode> redisNodes = new ArrayList<>();
            if (StringUtils.isNotBlank(redisProperties.getSentinel().getNodes())) {
                redisSentinelConfiguration = new RedisSentinelConfiguration();
                redisSentinelConfiguration.master(redisProperties.getSentinel().getMaster());
                node = redisProperties.getSentinel().getNodes().split(",");
                redisNodes = new ArrayList<>();
                RedisNode.RedisNodeBuilder redisNodeBuilder = null;
                String tmpStr = null;
                String[] tmpArr = null;
                int index = 0;
                for (String hostPort : node) {
                    tmpStr = null;
                    if (hostPort.contains("#")) { // master
                        tmpStr = hostPort.substring(hostPort.indexOf("#") + 1);
                        hostPort = hostPort.substring(0, hostPort.indexOf("#"));
                    }
                    tmpArr = hostPort.split(":");
                    redisNodeBuilder = new RedisNode.RedisNodeBuilder();
                    redisNodeBuilder.listeningAt(tmpArr[0], Integer.parseInt(tmpArr[1]));
                    if (StringUtils.isNotBlank(tmpStr)) { // master
                        redisNodeBuilder.withName(tmpStr);
                        redisNodeBuilder.withId(String.valueOf(index));
                        redisNodeBuilder.promotedAs(RedisNode.NodeType.MASTER);
                        redisNodes.add(redisNodeBuilder.build());
                    } else {
                        redisNodeBuilder.withId(String.valueOf(index));
                        redisNodeBuilder.promotedAs(RedisNode.NodeType.SLAVE);
                        redisNodes.add(redisNodeBuilder.build());
                    }
                    index++;
                }
            } else {
                redisSentinelConfiguration = new RedisSentinelConfiguration();
                redisSentinelConfiguration.master("master");
                node = redisProperties.getHost().split(":");
                RedisNode redisNode = new RedisNode(node[0], Integer.parseInt(node[1]));
                redisNode.setName("master");
                redisNodes.add(redisNode);
            }
            redisSentinelConfiguration.setSentinels(redisNodes);
            defaultLettucePool = new DefaultLettucePool(redisSentinelConfiguration);
            node = redisProperties.getHost().split(":");
            defaultLettucePool.setHostName(node[0]);
            defaultLettucePool.setPort(Integer.parseInt(node[1]));
            defaultLettucePool.setPassword(redisProperties.getPassword());
            defaultLettucePool.setDatabase(redisProperties.getDatabase());
            defaultLettucePool.setTimeout(redisProperties.getTimeout());
            defaultLettucePool.setPoolConfig(poolConfig);
            defaultLettucePool.afterPropertiesSet();
        }

        return defaultLettucePool;
    }

    @Bean
    @Primary
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

    /**
     * 标准集群主从分离方式－主
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
//    @Bean(name = "ledisMasterClusterConnection")
    @ConditionalOnBean(RedisProperties.class)
    public StatefulRedisClusterConnection ledisMasterClusterConnection() {
        if (null != redisProperties.getCluster()) {
            return null;
        }
        return this.getConnection(redisProperties.getCluster().getMaster(), ReadFrom.MASTER);
    }

    /**
     * 标准集群主从分离方式－从
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
//    @Bean(name = "ledisSlaveClusterConnection")
    @ConditionalOnBean(RedisProperties.class)
    public StatefulRedisClusterConnection ledisSlaveClusterConnection() {
        if (null != redisProperties.getCluster()) {
            return null;
        }
        return this.getConnection(redisProperties.getCluster().getSlave(), ReadFrom.SLAVE);
    }

    private StatefulRedisClusterConnection getConnection(RedisProperties.Cluster.Nodes nodes, ReadFrom readFrom) {
        StatefulRedisClusterConnection<String, String> connection = null;
        if (null != nodes) {
            String strNodes = nodes.getHost();
            String strPwd = nodes.getPassword();
            Long timeout = nodes.getTimeout();
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
                        .withPassword(strPwd).withDatabase(1)/*.withTimeout(timeout, TimeUnit.MILLISECONDS)*/.build();
                listRedisURIs.add(redisURI);
            }
            RedisClusterClient clusterClient = RedisClusterClient.create(listRedisURIs);
            try {
                connection = clusterClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (null != connection) {
                connection.setReadFrom(readFrom);
            }
        }
        return connection;
    }
}
