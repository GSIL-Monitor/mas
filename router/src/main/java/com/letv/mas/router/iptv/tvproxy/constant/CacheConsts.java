package com.letv.mas.router.iptv.tvproxy.constant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by leeco on 18/11/12.
 */
public class CacheConsts {

    /**
     * 缓存操作结果，0--失败，1--成功，-1--出错
     */
    public static final int SUCCESS = 1;
    public static final int FAIL = 0;
    public static final int ERROR = -1;

    /**
     * 对象转换为JSON字符串工具类
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }
}
