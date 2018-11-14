package com.letv.mas.router.iptv.tvproxy.model;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 需要存入缓存的对象类型信息封装类；
 * 当直接使用T.getclass时，可能丢失类型信息，这里封装一下，保存完整信息；
 *
 * @param <T>
 * @author KevinYi
 */
public class LetvTypeReference<T> extends TypeReference<T> {

}
