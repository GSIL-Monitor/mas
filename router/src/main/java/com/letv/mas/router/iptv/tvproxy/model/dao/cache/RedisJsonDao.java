package com.letv.mas.router.iptv.tvproxy.model.dao.cache;

import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.letv.mas.router.iptv.tvproxy.config.RedisConfig;
import com.letv.mas.router.iptv.tvproxy.constant.CacheConsts;
import com.letv.mas.router.iptv.tvproxy.model.LetvTypeReference;
import com.letv.mas.router.iptv.tvproxy.util.TimeUtil;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by leeco on 18/11/11.
 * Based on the lettuce client!
 */
@Lazy
@Component
@ConditionalOnBean(RedisConfig.class)
public class RedisJsonDao implements ICacheTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisJsonDao.class);
    private static final String LOG_TAG = "redis";

//    @Autowired
    private ApplicationContext applicationContext;

//    @SuppressWarnings("rawtypes")
//    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private GenericObjectPool masterPool;

    private GenericObjectPool slavePool;

    public RedisJsonDao(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        StatefulRedisClusterConnection redisClusterConnection = this.getMasterClient();
        if (null == redisClusterConnection) {
            masterPool = this.getMasterPool();
            if (null != masterPool) {
                slavePool = this.getSlavePool();
                LOGGER.debug("RedisJsonDao.init(): {}", masterPool);
            } else {
                redisTemplate = (RedisTemplate) applicationContext.getBean("redisCacheTemplate");
                LOGGER.debug("RedisJsonDao.init(): {}", redisTemplate);
            }

        } else {
            LOGGER.debug("RedisJsonDao.init(): {}", redisClusterConnection);
        }
    }

    public void destroy() {
        if (null != this.masterPool) {
            this.masterPool.close();
        }
        if (null != this.slavePool) {
            this.slavePool.close();
        }
    }

    public interface RedisCallBack {
        <T> T parser(String value);
    }

    private StatefulRedisClusterConnection getMasterClient() {
        return this.getClusterEntity("ledisMasterClusterConnection", StatefulRedisClusterConnection.class);
    }

    private StatefulRedisClusterConnection getSlaveClient() {
        return this.getClusterEntity("ledisSlaveClusterConnection", StatefulRedisClusterConnection.class);
    }

    private GenericObjectPool getMasterPool() {
        return this.getClusterEntity("ledisMasterPool", GenericObjectPool.class);
    }

    private GenericObjectPool getSlavePool() {
        return this.getClusterEntity("ledisSlavePool", GenericObjectPool.class);
    }

    private <T> T getClusterEntity(String beanName, Class<T> clazz) {
        T ret = null;
        try {
            Object bean = applicationContext.getBean(beanName);
            if (bean instanceof StatefulRedisClusterConnection) {
                ret = (T) bean;
                try {
                    ((StatefulRedisClusterConnection) bean).isOpen();
                } catch (Exception e) {
                    ret = null;
                }
            } else if (bean instanceof GenericObjectPool) {
                ret = (T) bean;
                try {
                    ((GenericObjectPool) bean).getRemoveAbandonedOnBorrow();
                } catch (Exception e) {
                    ret = null;
                }
            }
        } catch (Exception e) {

        }
        return ret;
    }

    @Override
    public int set(final String key, Object value) {
        return this.set(key, value, 0);
    }

    @Override
    public int set(String key, Object value, int duration) {
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("set").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            String jsonValue = null;
            if (value instanceof String) {
                jsonValue = (String) value;
            } else {
                jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
            }

            if (null != redisTemplate) {
                ValueOperations<String, String> operations = redisTemplate.opsForValue();
                if (duration > 0 || duration == -1) {
                    operations.set(key, jsonValue, duration, TimeUnit.SECONDS);
                } else {
                    operations.set(key, jsonValue);
                }
            } else {
                if (null != this.masterPool) {
                    poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                    if (duration > 0 || duration == -1) {
                        poolConnection.sync().setex(key, duration, jsonValue);
                    } else {
                        poolConnection.sync().set(key, jsonValue);
                    }
                } else {
                    clusterConnection = getMasterClient();
                    if (duration > 0 || duration == -1) {
                        clusterConnection.sync().setex(key, duration, jsonValue);
                    } else {
                        clusterConnection.sync().set(key, jsonValue);
                    }
                }
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.masterPool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }

    @Override
    public <T> int mset(Map<String, T> valueMap) {
        return this.mset(valueMap, 0);
    }

    @Override
    public <T> int mset(Map<String, T> valueMap, final int duration) {
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("mset").append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            String jsonValue = null;
            // 由于是Map，存在数据入出无序的情况！
            Map<String, String> jsonMap = new HashMap<>();
            String key = null;
            Object value = null;
            StringBuilder sbKeys = new StringBuilder();
            if (duration == 0) {
                for (Map.Entry<String, T> entry : valueMap.entrySet()) {
                    key = entry.getKey();
                    value = entry.getValue();
                    if (key != null && value != null) {
                        if (value instanceof String) {
                            jsonValue = (String) value;
                        } else {
                            jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
                        }
                        jsonMap.put(key, jsonValue);
                        sbKeys.append(key).append(",");
                    }
                }
                if (null != redisTemplate) {
                    redisTemplate.opsForValue().multiSet(jsonMap);
                } else {
                    if (null != this.masterPool) {
                        poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                        poolConnection.sync().mset(jsonMap);
                    } else {
                        clusterConnection = getMasterClient();
                        clusterConnection.sync().mset(jsonMap);
                    }
                    ret = CacheConsts.SUCCESS;
                }
            } else if (duration > 0 || duration == -1) {
                List<Object> batchData = null;
                if (null != redisTemplate) {
                    batchData = redisTemplate.executePipelined(new SessionCallback<List<Object>>() {
                        @Override
                        public List<Object> execute(RedisOperations operations) throws DataAccessException {
                            for (Map.Entry<String, T> entry : valueMap.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();
                                String jsonValue = null;
                                try {
                                    if (value instanceof String) {
                                        jsonValue = (String) value;
                                    } else {
                                        jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
                                    }
                                    operations.opsForValue().set(key, jsonValue, duration, TimeUnit.MILLISECONDS);
                                    sbKeys.append(key).append(",");
                                } catch (Exception e) {
                                }
                            }
                            return null;
                        }
                    });
                    if (null != batchData && batchData.size() > 0) {
                        ret = CacheConsts.SUCCESS;
                    }
                } else {
                    batchData = new ArrayList<Object>();
                    if (null != this.masterPool) {
                        poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                        poolConnection.sync().mset(jsonMap);
                        RedisAsyncCommands<String, String> commands = poolConnection.async();
                        commands.setAutoFlushCommands(false);
                        List<RedisFuture<String>> futures = new ArrayList<>();
                        for (Map.Entry<String, T> entry : valueMap.entrySet()) {
                            if (value instanceof String) {
                                jsonValue = (String) value;
                            } else {
                                jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
                            }
                            if (futures.add(commands.setex(key, duration, jsonValue))) {
                                batchData.add(key);
                            }
                        }
                        if (batchData.size() > 0) {
                            commands.setAutoFlushCommands(true);
                            commands.flushCommands();
                            ret = LettuceFutures.awaitAll(poolConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                    CacheConsts.SUCCESS : CacheConsts.FAIL;
                        }
                    } else {
                        clusterConnection = getMasterClient();
                        RedisClusterAsyncCommands<String, String> commands = clusterConnection.async();
                        commands.setAutoFlushCommands(false);
                        List<RedisFuture<String>> futures = new ArrayList<>();
                        for (Map.Entry<String, T> entry : valueMap.entrySet()) {
                            if (value instanceof String) {
                                jsonValue = (String) value;
                            } else {
                                jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
                            }
                            if (futures.add(commands.setex(key, duration, jsonValue))) {
                                batchData.add(key);
                            }
                        }
                        if (batchData.size() > 0) {
                            commands.setAutoFlushCommands(true);
                            commands.flushCommands();
                            ret = LettuceFutures.awaitAll(clusterConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                    CacheConsts.SUCCESS : CacheConsts.FAIL;
                        }
                    }
                    for (Object obj : batchData) {
                        sbKeys.append(obj).append(",");
                    }
                }
            }
            if (sbKeys.length() > 0) {
                sbKeys.deleteCharAt(sbKeys.length() - 1);
            }
            sb.append("key=").append(sbKeys).append(splitSymbol);
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.masterPool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return this.iGet(key, new RedisJsonDao.RedisCallBack() {
            @Override
            public <T> T parser(String value) {
                T ret = null;
                if (null != value) {
                    if (clazz == String.class) {
                        ret = (T) value;
                    } else {
                        try {
                            ret = CacheConsts.OBJECT_MAPPER.readValue(value, (Class<T>) clazz);
                        } catch (Exception e) {
                        }
                    }
                }
                return ret;
            }
        });
    }

    @Override
    public <T> T get(String key, LetvTypeReference<T> typeReference) {
        return this.iGet(key, new RedisJsonDao.RedisCallBack() {
            @Override
            public <T> T parser(String value) {
                T ret = null;
                if (null != value) {
                    try {
                        ret = CacheConsts.OBJECT_MAPPER.readValue(value, typeReference);
                    } catch (Exception e) {
                    }
                }
                return ret;
            }
        });
    }

    private <T> T iGet(String key, RedisCallBack redisCallBack) {
        T resultData = null;
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("get").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            String value = null;
            if (null != redisTemplate) {
                value = redisTemplate.opsForValue().get(key);
            } else {
                if (null != this.slavePool) {
                    poolConnection = (StatefulRedisConnection<String, String>) this.slavePool.borrowObject();
                    value = poolConnection.sync().get(key);
                } else {
                    clusterConnection = getSlaveClient();
                    value = clusterConnection.sync().get(key);
                }
            }
            if (null != redisCallBack) {
                resultData = redisCallBack.parser(value);
            } else {
                resultData = CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>());
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.slavePool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return resultData;
    }

    @Override
    public <T> Map<String, T> mget(List<String> keys, Class<T> clazz) {
        return mget(keys, clazz, 0);
    }

    @Override
    public <T> Map<String, T> mget(List<String> keys, Class<T> clazz, int batchSize) {
        return this.iMGet(keys, new RedisJsonDao.RedisCallBack() {
            @Override
            public <T> T parser(String value) {
                T ret = null;
                if (null != value) {
                    if (clazz == String.class) {
                        ret = (T) value;
                    } else {
                        try {
                            ret = CacheConsts.OBJECT_MAPPER.readValue(value, (Class<T>) clazz);
                        } catch (Exception e) {
                        }
                    }
                }
                return ret;
            }
        }, batchSize);
    }

    @Override
    public <T> Map<String, T> mget(List<String> keys, LetvTypeReference<T> typeReference) {
        return this.iMGet(keys, new RedisJsonDao.RedisCallBack() {
            @Override
            public <T> T parser(String value) {
                T ret = null;
                if (null != value) {
                    try {
                        ret = CacheConsts.OBJECT_MAPPER.readValue(value, typeReference);
                    } catch (Exception e) {
                    }
                }
                return ret;
            }
        }, 0);
    }

    private <T> Map<String, T> iMGet(List<String> keys, RedisCallBack redisCallBack, int batchSize) {
        int ret = CacheConsts.FAIL;
        final Map<String, T> resultData = new HashMap<>(keys.size());
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("mget").append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            StringBuilder sbKeys = new StringBuilder();
            List<Object> batchData = null;
            if (batchSize > 0) {
                int pageNumber = batchSize; // 每页记录数
                int totalCount = keys.size(); // 总记录数
                int totalPage = totalCount / pageNumber; // 总页数
                if ((totalCount % pageNumber) > 0) {
                    totalPage += 1;
                }
                int fromIndex = 0; // 起始位置
                int toIndex = 0; // 结束位置
                for (int pageNo = 0; pageNo < totalPage; pageNo++) {
                    fromIndex = pageNo * pageNumber;
                    toIndex = ((pageNo + 1) * pageNumber);
                    if (toIndex > totalCount) {
                        toIndex = totalCount;
                    }
                    final List<String> tmpList = keys.subList(fromIndex, toIndex);
                    if (null != redisTemplate) {
                        batchData = redisTemplate.executePipelined(new SessionCallback<List<Object>>() {
                            @Override
                            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                                List<Object> retKeys = new ArrayList<Object>();
                                for (String key : tmpList) {
                                    operations.opsForValue().get(key);
                                }
                                return null;
                            }
                        });
                        if (null != batchData && batchData.size() == tmpList.size()) {
                            String value = null;
                            for (int i = 0; i < batchData.size(); i++) {
                                try {
                                    value = (String) batchData.get(i);
                                    if (null != value) {
                                        if (null != redisCallBack) {
                                            resultData.put(tmpList.get(i), redisCallBack.parser(value));
                                        } else {
                                            resultData.put(tmpList.get(i), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                        for (Object obj : tmpList) { // keys
                            sbKeys.append(obj).append(",");
                        }
                    } else {
                        batchData = new ArrayList<Object>();
                        if (null != this.slavePool) {
                            poolConnection = (StatefulRedisConnection<String, String>) this.slavePool.borrowObject();
                            RedisAsyncCommands<String, String> commands = poolConnection.async();
                            commands.setAutoFlushCommands(false);
                            List<RedisFuture<String>> futures = new ArrayList<>();
                            String value = null;
                            int index = 0;
                            for (String key : tmpList) {
                                if (futures.add(commands.get(key))) {
                                    batchData.add(key);
                                }
                            }
                            if (batchData.size() > 0) {
                                commands.setAutoFlushCommands(true);
                                commands.flushCommands();
                                ret = LettuceFutures.awaitAll(poolConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                        CacheConsts.SUCCESS : CacheConsts.FAIL;
                                if (CacheConsts.SUCCESS == ret) {
                                    for (RedisFuture future : futures) {
                                        value = String.valueOf(future.get());
                                        if (null != value) {
                                            if (null != redisCallBack) {
                                                resultData.put(String.valueOf(batchData.get(index++)), redisCallBack.parser(value));
                                            } else {
                                                resultData.put(String.valueOf(batchData.get(index++)), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                            }
                                        }
                                    }
                                }
                            }
                            futures = null;
                        } else {
                            clusterConnection = getSlaveClient();
                            RedisClusterAsyncCommands<String, String> commands = clusterConnection.async();
                            commands.setAutoFlushCommands(false);
                            List<RedisFuture<String>> futures = new ArrayList<>();
                            String value = null;
                            int index = 0;
                            for (String key : tmpList) {
                                if (futures.add(commands.get(key))) {
                                    batchData.add(key);
                                }
                            }
                            if (batchData.size() > 0) {
                                commands.setAutoFlushCommands(true);
                                commands.flushCommands();
                                ret = LettuceFutures.awaitAll(clusterConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                        CacheConsts.SUCCESS : CacheConsts.FAIL;
                                if (CacheConsts.SUCCESS == ret) {
                                    for (RedisFuture future : futures) {
                                        value = String.valueOf(future.get());
                                        if (null != value) {
                                            if (null != redisCallBack) {
                                                resultData.put(String.valueOf(batchData.get(index++)), redisCallBack.parser(value));
                                            } else {
                                                resultData.put(String.valueOf(batchData.get(index++)), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                            }
                                        }
                                    }
                                }
                            }
                            futures = null;
                        }
                    }
                    for (Object obj : batchData) {
                        sbKeys.append(obj).append(",");
                    }
                }
            } else {
                if (null != redisTemplate) {
                    batchData = redisTemplate.executePipelined(new SessionCallback<List<Object>>() {
                        @Override
                        public <K, V> List<Object> execute(RedisOperations<K, V> operations) throws DataAccessException {
                            List<Object> retKeys = new ArrayList<Object>();
                            for (String key : keys) {
                                operations.opsForValue().get(key);
                            }
                            return null;
                        }
                    });
                    if (null != batchData && batchData.size() == keys.size()) {
                        String value = null;
                        for (int i = 0; i < batchData.size(); i++) {
                            try {
                                value = (String) batchData.get(i);
                                if (null != value) {
                                    if (null != redisCallBack) {
                                        resultData.put(keys.get(i), redisCallBack.parser(value));
                                    } else {
                                        resultData.put(keys.get(i), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                    }
                                    sbKeys.append(keys.get(i)).append(",");
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    batchData = new ArrayList<Object>();
                    if (null != this.slavePool) {
                        poolConnection = (StatefulRedisConnection<String, String>) this.slavePool.borrowObject();
                        RedisAsyncCommands<String, String> commands = poolConnection.async();
                        commands.setAutoFlushCommands(false);
                        List<RedisFuture<String>> futures = new ArrayList<>();
                        String value = null;
                        int index = 0;
                        for (String key : keys) {
                            if (futures.add(commands.get(key))) {
                                batchData.add(key);
                            }
                        }
                        if (batchData.size() > 0) {
                            commands.setAutoFlushCommands(true);
                            commands.flushCommands();
                            ret = LettuceFutures.awaitAll(poolConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                    CacheConsts.SUCCESS : CacheConsts.FAIL;
                            if (CacheConsts.SUCCESS == ret) {
                                for (RedisFuture future : futures) {
                                    value = String.valueOf(future.get());
                                    if (null != value) {
                                        if (null != redisCallBack) {
                                            resultData.put(String.valueOf(batchData.get(index++)), redisCallBack.parser(value));
                                        } else {
                                            resultData.put(String.valueOf(batchData.get(index++)), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                        }
                                    }
                                }
                            }
                        }
                        futures = null;
                    } else {
                        clusterConnection = getSlaveClient();
                        RedisClusterAsyncCommands<String, String> commands = clusterConnection.async();
                        commands.setAutoFlushCommands(false);
                        List<RedisFuture<String>> futures = new ArrayList<>();
                        String value = null;
                        int index = 0;
                        for (String key : keys) {
                            if (futures.add(commands.get(key))) {
                                batchData.add(key);
                            }
                        }
                        if (batchData.size() > 0) {
                            commands.setAutoFlushCommands(true);
                            commands.flushCommands();
                            ret = LettuceFutures.awaitAll(clusterConnection.getTimeout(), TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futures.size()])) ?
                                    CacheConsts.SUCCESS : CacheConsts.FAIL;
                            if (CacheConsts.SUCCESS == ret) {
                                for (RedisFuture future : futures) {
                                    value = String.valueOf(future.get());
                                    if (null != value) {
                                        if (null != redisCallBack) {
                                            resultData.put(String.valueOf(batchData.get(index++)), redisCallBack.parser(value));
                                        } else {
                                            resultData.put(String.valueOf(batchData.get(index++)), CacheConsts.OBJECT_MAPPER.readValue(value, new LetvTypeReference<T>()));
                                        }
                                    }
                                }
                            }
                        }
                        futures = null;
                    }
                    for (Object obj : batchData) {
                        sbKeys.append(obj).append(",");
                    }
                }
            }
            if (sbKeys.length() > 0) {
                sbKeys.deleteCharAt(sbKeys.length() - 1);
            }
            sb.append("key=").append(sbKeys).append(splitSymbol);
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.slavePool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return resultData;
    }

    @Override
    public int delete(String key) {
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("delete").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            if (null != redisTemplate) {
                redisTemplate.delete(key);
            } else {
                if (null != this.masterPool) {
                    poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                    poolConnection.sync().del(key);
                } else {
                    clusterConnection = getMasterClient();
                    clusterConnection.sync().del(key);
                }
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.masterPool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }

    @Override
    public int sismember(String key, String member) {
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("sismember").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            if (null != redisTemplate) {
                redisTemplate.opsForSet().isMember(key, member);
            } else {
                if (null != this.masterPool) {
                    poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                    poolConnection.sync().sismember(key, member);
                } else {
                    clusterConnection = getSlaveClient();
                    clusterConnection.sync().sismember(key, member);
                }
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.masterPool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }

    @Override
    public int sadd(String key, Set<String> member) {
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(TimeUtil.getISO8601Timestamp(new Date())).append(splitSymbol)
                .append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("sadd").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);
        StatefulRedisClusterConnection<String, String> clusterConnection = null;
        StatefulRedisConnection<String, String> poolConnection = null;

        try {
            if (null != redisTemplate) {
                redisTemplate.opsForSet().add(key, member.toArray(new String[member.size()]));
            } else {
                if (null != this.masterPool) {
                    poolConnection = (StatefulRedisConnection<String, String>) this.masterPool.borrowObject();
                    poolConnection.sync().sadd(key, member.toArray(new String[member.size()]));
                } else {
                    clusterConnection = getMasterClient();
                    clusterConnection.sync().sadd(key, member.toArray(new String[member.size()]));
                }
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        } finally {
            if (null != clusterConnection) {
                clusterConnection.close();
            }
            if (null != poolConnection) {
                this.masterPool.returnObject(poolConnection);
            }
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }
}
