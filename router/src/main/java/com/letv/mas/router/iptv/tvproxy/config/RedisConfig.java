package com.letv.mas.router.iptv.tvproxy.config;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.support.ConnectionPoolSupport;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
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

    @Autowired
    LedisPoolConfig ledisPoolConfig;

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
        private Sentinel sentinel;

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
            private String mode;
            private Nodes master;
            private Nodes slave;

            @Data
            public static class Nodes {
                private String host;
                private String password;
                private Long timeout;
            }
        }

        @Data
        public static class Sentinel {
            private String master;
            private String nodes;
        }
    }

    public class LedisPoolConfig extends GenericObjectPoolConfig {}
    @Bean
    public LedisPoolConfig getRedisConfig() {
        LedisPoolConfig genericObjectPoolConfig = new LedisPoolConfig();
        genericObjectPoolConfig.setJmxEnabled(false);
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
    public DefaultLettucePool getDefaultLettucePool(LedisPoolConfig poolConfig) {
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

    /**
     * 本地data缓存
     * @param redisTemplate
     * @return
     */
    @Bean
    public RedisCacheManager getRedisCacheManager(RedisTemplate redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }

    /**
     * 非标准集群主从分离方式－主
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
    @Primary
    @Bean(name = "ledisMasterPool")
    @ConditionalOnBean(LedisPoolConfig.class)
    @ConditionalOnProperty(value = {"spring.redis.cluster.mode"}, havingValue = "seperate", matchIfMissing = false)
    public GenericObjectPool ledisMasterPool(LedisPoolConfig poolConfig) {
        if (null == redisProperties.getCluster()) {
            return null;
        }
        return this.getClusterPool(redisProperties.getCluster().getSlave(), poolConfig);
    }

    /**
     * 非标准集群主从分离方式－从
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
    @Bean(name = "ledisSlavePool")
    @ConditionalOnBean(LedisPoolConfig.class)
    @ConditionalOnProperty(value = {"spring.redis.cluster.mode"}, havingValue = "seperate", matchIfMissing = false)
    public GenericObjectPool ledisSlavePool(LedisPoolConfig poolConfig) {
        if (null == redisProperties.getCluster()) {
            return null;
        }
        return this.getClusterPool(redisProperties.getCluster().getSlave(), poolConfig);
    }

    /**
     * 标准集群主从分离方式－主
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
    @Bean(name = "ledisMasterClusterConnection")
    @ConditionalOnBean(RedisProperties.class)
    @ConditionalOnProperty(value = {"spring.redis.cluster.mode"}, havingValue = "normal", matchIfMissing = false)
    public StatefulRedisClusterConnection ledisMasterClusterConnection() {
        if (null == redisProperties.getCluster()) {
            return null;
        }
        return this.getClusterConnection(redisProperties.getCluster().getMaster(), ReadFrom.MASTER);
    }

    /**
     * 标准集群主从分离方式－从
     * @return
     */
    @SuppressWarnings("unchecked")
//    @RefreshScope
    @Bean(name = "ledisSlaveClusterConnection")
    @ConditionalOnBean(RedisProperties.class)
    @ConditionalOnProperty(value = {"spring.redis.cluster.mode"}, havingValue = "normal", matchIfMissing = false)
    public StatefulRedisClusterConnection ledisSlaveClusterConnection() {
        if (null == redisProperties.getCluster()) {
            return null;
        }
        return this.getClusterConnection(redisProperties.getCluster().getSlave(), ReadFrom.SLAVE);
    }

    /**
     * 获取标准主从连接
     * @param nodes
     * @param readFrom
     * @return
     */
    private StatefulRedisClusterConnection getClusterConnection(RedisProperties.Cluster.Nodes nodes, ReadFrom readFrom) {
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

    /**
     * 获取非标准主从连接池
     * @param nodes
     * @return
     */
    private GenericObjectPool getClusterPool(RedisProperties.Cluster.Nodes nodes, LedisPoolConfig poolConfig) {
        GenericObjectPool<StatefulRedisConnection<String, String>> pool = null;

        if (null != nodes) {
            StatefulRedisConnection<String, String> connection = null;
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
                        .withPassword(strPwd).withDatabase(redisProperties.getDatabase())
                        .withTimeout(timeout, TimeUnit.MILLISECONDS).build();
                listRedisURIs.add(redisURI);
            }
            RedisClient clusterClient = RedisClient.create(listRedisURIs.get(0));
            pool = ConnectionPoolSupport.createGenericObjectPool(() -> clusterClient.connect(), poolConfig);
        }

        return pool;
    }
}
