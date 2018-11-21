package com.letv.mas.router.iptv.tvproxy.plugin.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Created by leeco on 18/11/19.
 */
@Data
@Primary
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
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
