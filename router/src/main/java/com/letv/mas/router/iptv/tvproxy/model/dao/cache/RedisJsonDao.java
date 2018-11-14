package com.letv.mas.router.iptv.tvproxy.model.dao.cache;

import com.letv.mas.router.iptv.tvproxy.constant.CacheConsts;
import com.letv.mas.router.iptv.tvproxy.model.LetvTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by leeco on 18/11/11.
 */
public class RedisJsonDao implements ICacheTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisJsonDao.class);
    private static final String LOG_TAG = "redis";

    @SuppressWarnings("rawtypes")
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
        sb.append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("set").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);

        try {
            String jsonValue = null;
            if (value instanceof String) {
                jsonValue = (String) value;
            } else {
                jsonValue = CacheConsts.OBJECT_MAPPER.writeValueAsString(value);
            }
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            operations.set(key, jsonValue);
            if (duration > 0 || duration == -1) {
                redisTemplate.expire(key, duration, TimeUnit.SECONDS);
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
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
        sb.append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("mset").append(splitSymbol);

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
                redisTemplate.opsForValue().multiSet(jsonMap);
            } else if (duration > 0 || duration == -1) {
                List<Object> retKeys = redisTemplate.executePipelined(new RedisCallback<List<Object>>() {
                    @Override
                    public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
                        List<Object> retKeys = new ArrayList<Object>();
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
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
                                stringRedisConn.setEx(key, duration > 0 ? duration * 1000 : duration, jsonValue);
                                retKeys.add(key);
                            } catch (Exception e) {
                            }
                        }
                        return retKeys;
                    }
                });
                for (Object obj : retKeys) {
                    sbKeys.append(obj).append(",");
                }
            }
            if (sbKeys.length() > 0) {
                sbKeys.deleteCharAt(sbKeys.length() - 1);
            }
            sb.append("key=").append(sbKeys).append(splitSymbol);
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return ret;
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        T resultData = null;
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("get").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (null != value) {
                if (clazz == String.class) {
                    resultData = (T) value;
                } else {
                    resultData = CacheConsts.OBJECT_MAPPER.readValue(value, clazz);
                }
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return resultData;
    }

    @Override
    public <T> T get(String key, LetvTypeReference<T> typeReference) {
        T resultData = null;
        int ret = CacheConsts.FAIL;
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("get").append(splitSymbol)
                .append("key=").append(key).append(splitSymbol);

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (null != value) {
                resultData = CacheConsts.OBJECT_MAPPER.readValue(value, typeReference);
            }
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
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
        int ret = CacheConsts.FAIL;
        final Map<String, T> resultData = new HashMap<>(keys.size());
        long stime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        String splitSymbol = "|";
        sb.append(RedisJsonDao.LOG_TAG).append(splitSymbol)
                .append("mget").append(splitSymbol);

        try {
            StringBuilder sbKeys = new StringBuilder();
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
                    final Map<String, T> tmpMap = new HashMap<>(tmpList.size());
                    List<Object> retKeys = redisTemplate.executePipelined(new RedisCallback<List<Object>>() {
                        @Override
                        public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
                            List<Object> retKeys = new ArrayList<Object>();
                            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                            String value = null;
                            for (String key : tmpList) {
                                try {
                                    value = redisTemplate.opsForValue().get(key);
                                    if (null != value) {
                                        if (clazz == String.class) {
                                            tmpMap.put(key, (T) value);
                                        } else {
                                            tmpMap.put(key, CacheConsts.OBJECT_MAPPER.readValue(value, clazz));
                                        }
                                        retKeys.add(key);
                                    }
                                } catch (Exception e) {
                                }
                            }
                            return retKeys;
                        }
                    });
                    if (tmpMap.size() > 0) {
                        resultData.putAll(tmpMap);
                    }
                    for (Object obj : retKeys) {
                        sbKeys.append(obj).append(",");
                    }
                }
            } else {
                List<Object> retKeys = redisTemplate.executePipelined(new RedisCallback<List<Object>>() {
                    @Override
                    public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
                        List<Object> retKeys = new ArrayList<Object>();
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        String value = null;
                        for (String key : keys) {
                            try {
                                value = redisTemplate.opsForValue().get(key);
                                if (null != value) {
                                    if (clazz == String.class) {
                                        resultData.put(key, (T) value);
                                    } else {
                                        resultData.put(key, CacheConsts.OBJECT_MAPPER.readValue(value, clazz));
                                    }
                                    retKeys.add(key);
                                }
                            } catch (Exception e) {
                            }
                        }
                        return retKeys;
                    }
                });
                for (Object obj : retKeys) {
                    sbKeys.append(obj).append(",");
                }
            }
            if (sbKeys.length() > 0) {
                sbKeys.deleteCharAt(sbKeys.length() - 1);
            }
            sb.append("key=").append(sbKeys).append(splitSymbol);
            ret = CacheConsts.SUCCESS;
        } catch (Exception e) {
            ret = CacheConsts.ERROR;
        }

        sb.append(System.currentTimeMillis() - stime).append(splitSymbol)
                .append(ret);
        LOGGER.info(sb.toString());
        return resultData;
    }

    @Override
    public <T> Map<String, T> mget(List<String> keys, LetvTypeReference<T> typeReference) {
        return null;
    }

    @Override
    public int delete(String key) {
        return 0;
    }

    @Override
    public int sismember(String key, String member) {
        return 0;
    }

    @Override
    public int sadd(String key, Set<String> member) {
        return 0;
    }
}
