package com.letv.mas.caller.iptv.tvproxy.common.util;

import com.letv.mas.caller.iptv.tvproxy.common.plugin.SessionCache;
import com.letv.mas.caller.iptv.tvproxy.common.plugin.TpServiceStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClientUtil extends RestTemplate {
    private final Log logger = LogFactory.getLog("httpClientLog");
    private final Log apiSwitchMonitor = LogFactory.getLog("apiSwitchMonitorLog");
    private final Log serviceStatusLoger = LogFactory.getLog("serviceStatusLog");

    //@Autowired
    //private SessionCache sessionCache;

    // protected static SwitchTpDaoMBean switchTpDaoMBean =
    // JmxClient.getBean(SwitchTpDao.class);

    @Autowired
    MeterRegistry registry;

    /*
     * 第三方服务状态集
     */
    private final static Map<String, TpServiceStatus> tpServerStatusMap = new HashMap<>();
    private static long INTERFACE_RESPONSE_TIME = 2000;// 第三方接口响应时间临界值 2s
    //private static long INTERFACE_RESPONSE_TIME = 500;

    static {
        /* 临时处理，防止定时切服 at wangshengkai */
        String switchTp = System.getProperty("switch_tp");
        if ("true".equals(switchTp)) {
            INTERFACE_RESPONSE_TIME = Integer.MAX_VALUE;
        }
        System.out.println("***************INTERFACE_RESPONSE_TIME=" + INTERFACE_RESPONSE_TIME);
    }

    public static Map<String, TpServiceStatus> getServerStatusMap(){
        return tpServerStatusMap;
    }

    private static final int INTERFACE_TOTAL_COUNT = 10;
    private static final long INTERFACE_RECORD_COUNT_RESET_TIME = 1 * 60 * 1000;
    private static final long INTERFACE_AUTO_NORMAL_TIME = 5 * 60 * 1000;// 接口自动恢复时间5分钟

    public HttpClientUtil() {
        super();
    }

    public HttpClientUtil(ClientHttpRequestFactory requestFactory) {
        super();
        this.setRequestFactory(requestFactory);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... urlVariables) {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        /*if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, urlVariables), "", logInfo);
            }
            return null;
        }*/

        /*
         * 第三方服务可用，执行请求
         */
        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        String completeUrl = this.getUrl(url, urlVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.getForEntity(url, responseType, urlVariables);// 执行请求
        } catch (Exception e) {
//            if (domain.startsWith("http://api.message.le.com/v1/messages")) { //TC
//                this.logger.error("exception url:" + getUrl(url, urlVariables) +
//                        ",time:"
//                        + + (System.currentTimeMillis() - stime) + "|0");
//                logger.info(e.getMessage(), e);
//            }
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {// 记录日志
            HttpStatus status = response.getStatusCode();
            if (completeUrl != null && completeUrl.length() > 1024) {
                completeUrl = url + "...params to be ignored";
            }

            // this.logger.warn("execute url:" + urlTmp + " status:" +
            // status.name() + ",statusname:" + status.value()
            // + ",time:" + wasteTime);
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            /*if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }*/
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + urlTmp + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }

    public <T> ResponseEntity<T> getForEntity(SessionCache sessionCache,String url, Class<T> responseType, Object... urlVariables) {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        /*if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != sessionCache) {
                String logInfo = "|505|0|" + "0|0";
                sessionCache.setResponse(this.getUrl(url, urlVariables), "", logInfo);
            }
            return null;
        }*/

        /*
         * 第三方服务可用，执行请求
         */
        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        String completeUrl = this.getUrl(url, urlVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.getForEntity(url, responseType, urlVariables);// 执行请求
        } catch (Exception e) {
//            if (domain.startsWith("http://api.message.le.com/v1/messages")) { //TC
//                this.logger.error("exception url:" + getUrl(url, urlVariables) +
//                        ",time:"
//                        + + (System.currentTimeMillis() - stime) + "|0");
//                logger.info(e.getMessage(), e);
//            }
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != sessionCache) {
                sessionCache.setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {// 记录日志
            HttpStatus status = response.getStatusCode();
            if (completeUrl != null && completeUrl.length() > 1024) {
                completeUrl = url + "...params to be ignored";
            }

            // this.logger.warn("execute url:" + urlTmp + " status:" +
            // status.name() + ",statusname:" + status.value()
            // + ",time:" + wasteTime);
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            /*if (null != sessionCache) {
                sessionCache.setResponse(completeUrl, response.getBody(), logInfo);
            }*/
            SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
        } else {
            // this.logger.warn("execute url:" + urlTmp + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != sessionCache) {
                sessionCache.setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }


    @Override
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> urlVariables) {

        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        /*if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, urlVariables), "", logInfo);
            }
            return null;
        }*/

        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        fillUrlVariables(url, urlVariables);
        String completeUrl = this.getUrl(url, urlVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.getForEntity(url, responseType, urlVariables);// 执行请求
        } catch (Exception e) {
            // this.logger.error("exception url:" + getUrl(url, urlVariables) +
            // ",time:"
            // + + (System.currentTimeMillis() - stime) + "|0");
            // logger.info(e.getMessage(), e);
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {
            HttpStatus status = response.getStatusCode();
            if (completeUrl != null && completeUrl.length() > 1024) {
                completeUrl = completeUrl + "...params to be ignored";
            }

            // this.logger.warn("execute url:" + urlTmp + " status:" +
            // status.name() + ",statusname:" + status.value()
            // + ",time:" + wasteTime);
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + urlTmp + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        try {
            response = super.getForEntity(url, responseType);
        } catch (RestClientException e) {
            // this.logger.error("exception url:" + url + ",time:" +
            // + (System.currentTimeMillis() - stime) );
            // logger.info(e.getMessage(), e);
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }
        if (response != null) {
            HttpStatus status = response.getStatusCode();
            // this.logger.warn("execute url:" + url + " status:" +
            // status.name() + ",statusname:" + status.value()
            // + ",time:" + + (System.currentTimeMillis() - stime) + "|0");
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + url + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }
        return response;
    }

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                          Class<T> responseType, Object... uriVariables) {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        /*if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, uriVariables), "", logInfo);
            }
            return null;
        }*/

        /*
         * 第三方服务可用，执行请求
         */
        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        String completeUrl = this.getUrl(url, uriVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.exchange(completeUrl, method, requestEntity, responseType, uriVariables);// 执行请求
        } catch (Exception e) {
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {// 记录日志
            HttpStatus status = response.getStatusCode();
            if (completeUrl != null && completeUrl.length() > 1024) {
                completeUrl = url + "...params to be ignored";
            }

            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }
        } else {
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                          Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);
        TpServiceStatus tpServerStatus = this.tpServerStatusMap.get(domain);
        if (tpServerStatus == null) {// 未初始化
            synchronized (this.tpServerStatusMap) {// 防止多个线程重复初始化
                if (tpServerStatus == null) {
                    tpServerStatus = new TpServiceStatus();

                    this.tpServerStatusMap.put(domain, tpServerStatus);
                }
            }
        }

        /*
         * 验证第三方服务是否可用
         */
        if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, uriVariables), "", logInfo);
            }
            return null;
        }

        /*
         * 第三方服务可用，执行请求
         */
        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        fillUrlVariables(url, uriVariables);
        String completeUrl = this.getUrl(url, uriVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.exchange(completeUrl, method, requestEntity, responseType, uriVariables);// 执行请求
        } catch (Exception e) {
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {// 记录日志
            HttpStatus status = response.getStatusCode();
            if (completeUrl != null && completeUrl.length() > 1024) {
                completeUrl = url + "...params to be ignored";
            }

            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }
        } else {
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }

    @Override
    public <T> T getForObject(String url, Class<T> responseType, Object... urlVariables) {
        T response = null;
        if (StringUtils.isEmpty(url)) {
            return response;
        }
        ResponseEntity<T> responseEntity = this.getForEntity(url, responseType, urlVariables);
        if (responseEntity != null) {
            response = responseEntity.getBody();
        }
        return response;
    }

    public <T> T getForObject(SessionCache sessionCache,String url, Class<T> responseType, Object... urlVariables) {
        T response = null;
        if (StringUtils.isEmpty(url)) {
            return response;
        }
        ResponseEntity<T> responseEntity = this.getForEntity(sessionCache,url, responseType, urlVariables);
        if (responseEntity != null) {
            response = responseEntity.getBody();
        }
        return response;
    }

    @Override
    public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> urlVariables) {
        T response = null;
        if (StringUtils.isEmpty(url)) {
            return response;
        }
        ResponseEntity<T> responseEntity = this.getForEntity(url, responseType, urlVariables);
        if (responseEntity != null) {
            response = responseEntity.getBody();
        }
        return response;
    }

    @Override
    public <T> T getForObject(URI url, Class<T> responseType) {
        T response = null;
        if (url == null) {
            return response;
        }
        ResponseEntity<T> responseEntity = this.getForEntity(url, responseType);
        if (responseEntity != null) {
            response = responseEntity.getBody();
        }
        return response;
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
                                               Object... uriVariables) throws RestClientException {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, uriVariables), "", logInfo);
            }
            return null;
        }

        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        String completeUrl = this.getUrl(url, uriVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.postForEntity(url, request, responseType, uriVariables);
        } catch (Exception e) {
            // this.logger.error("exception url:" + url + ",params:" +
            // this.arrayToString(uriVariables) + ",time:"
            // + + (System.currentTimeMillis() - stime) + "|0");
            // logger.error(e.getMessage(), e);
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {
            HttpStatus status = response.getStatusCode();
            // String params = this.arrayToString(uriVariables);
            // if (params != null && params.length() > 1024) {
            // params = "ignored";
            // }
            // this.logger.warn("execute url:" + url + " params:" + params +
            // " status:" + status.name() + ",statusname:"
            // + status.value() + ",time:" + (System.currentTimeMillis() -
            // stime));
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + url + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(completeUrl + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(completeUrl, "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);
        return response;
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(URI url, Object request, Class<T> responseType)
            throws RestClientException {
        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url.toString());// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
            return null;
        }

        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        try {
            response = super.postForEntity(url, request, responseType);
        } catch (Exception e) {
            // this.logger.error("exception url:" + url + ",time:" +
            // + (System.currentTimeMillis() - stime) );
            // logger.error(e.getMessage(), e);
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {
            HttpStatus status = response.getStatusCode();
            // this.logger.warn("execute url:" + url + "request:" + request +
            // " status:" + status.name() + ",statusname:"
            // + status.value() + ",time:" + (System.currentTimeMillis() -
            // stime));
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + url + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);
        return response;
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType,
                                               Map<String, ?> uriVariables) throws RestClientException {

        /*
         * 初始化第三方服务状态对象
         */
        String domain = this.getUrlTemplate(url);// this.getDomain(url);
        TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

        /*
         * 验证第三方服务是否可用
         */
        if ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME) {// 距离最近一次关闭不到设定的时间间隔，直接返回
            if (null != SessionCache.getSession()) {
                String logInfo = "|505|0|" + "0|0";
                SessionCache.getSession().setResponse(this.getUrl(url, uriVariables), "", logInfo);
            }
            return null;
        }

        long stime = System.currentTimeMillis();
        ResponseEntity<T> response = null;
        fillUrlVariables(url, uriVariables);
        String completeUrl = this.getUrl(url, uriVariables); // 已经拼接好的完整url，用于日志
        try {
            response = super.postForEntity(url, request, responseType, uriVariables);
        } catch (Exception e) {
            // this.logger.error("exception url:" + url + ",time:" +
            // + (System.currentTimeMillis() - stime) );
            // logger.error(e.getMessage(), e);
            String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }
        long wasteTime = System.currentTimeMillis() - stime;
        if (response != null) {
            HttpStatus status = response.getStatusCode();
            // this.logger.warn("execute url:" + url + " params:" + uriVariables
            // + " status:" + status.name()
            // + ",statusname:" + status.value() + ",time:" +
            // + (System.currentTimeMillis() - stime) );
            String logInfo = "|" + status.value() + "|" + response.toString().getBytes().length + "|"
                    + +(System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), response.getBody(), logInfo);
            }
        } else {
            // this.logger.warn("execute url:" + url + ",http response:" +
            // response);
            String logInfo = "|100|0|" + (System.currentTimeMillis() - stime) + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }

        /*
         * 验证第三方服务状态
         */
        this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);

        return response;
    }

    @Override
    public <T> T postForObject(String url, Object request, Class<T> responseType, Map<String, ?> uriVariables)
            throws RestClientException {
        T response = null;
        if (StringUtils.isEmpty(url)) {
            return response;
        }
        ResponseEntity<T> postForEntity = this.postForEntity(url, request, responseType, uriVariables);
        if (postForEntity != null) {
            response = postForEntity.getBody();
        }
        return response;
    }

    @Override
    public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables)
            throws RestClientException {
        T response = null;
        if (StringUtils.isEmpty(url)) {
            return response;
        }
        ResponseEntity<T> postForEntity = this.postForEntity(url, request, responseType, uriVariables);
        if (postForEntity != null) {
            response = postForEntity.getBody();
        }
        return response;
    }

    @Override
    public <T> T postForObject(URI url, Object request, Class<T> responseType) throws RestClientException {
        T response = null;
        if (url == null) {
            return response;
        }
        ResponseEntity<T> postForEntity = this.postForEntity(url, request, responseType);
        if (postForEntity != null) {
            response = postForEntity.getBody();
        }
        return response;
    }

    private String arrayToString(Object... urlVariables) {
        StringBuffer rsult = new StringBuffer();
        if (urlVariables != null) {
            for (Object value : urlVariables) {
                rsult.append(value).append(", ");
            }
        }
        return rsult.toString();
    }

    private String getUrl(String url, Map<String, ?> params) {
        try {
            if (params != null) {
                for (String key : params.keySet()) {
                    url = url.replace("{" + key + "}", params.get(key) == null ? "" : params.get(key).toString());
                }
            }
        } catch (Exception e) {
            url = url + " params:" + this.arrayToString(params);
        }
        return url;
    }

    private String getUrl(String url, Object[] params) {
        try {
            if (params != null) {
                Pattern p = Pattern.compile("\\{\\w+\\}");
                Matcher m = p.matcher(url);
                int i = 0;
                while (m.find()) {
                    url = url.replace(m.group(0), params[i] == null ? "" : params[i].toString());
                    i++;
                }
            }
        } catch (Exception e) {
            url = url + " params:" + this.arrayToString(params);
        }
        return url;
    }

    private String getDomain(String url) {
        Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group();
        }

        return "";
    }

    private String getUrlTemplate(String url) {
        long l = System.currentTimeMillis();
        String urlTmp = ApiSwitchStrategyUtil.getUrlTemp(url);
        long waste = System.currentTimeMillis() - l;
        if (waste > 20) {
            this.logger.info("getUrlTemplate waste=======:" + waste);
        }
        return urlTmp;
    }

    private TpServiceStatus getTpServiceStatus(String domain) {
        TpServiceStatus tpServerStatus = tpServerStatusMap.get(domain);
        if (tpServerStatus == null) {// 未初始化
            synchronized (tpServerStatusMap) {// 防止多个线程重复初始化
                if (tpServerStatusMap.get(domain) == null) {
                    tpServerStatus = new TpServiceStatus();
                    this.tpServerStatusMap.put(domain, tpServerStatus);
                    Gauge.builder("net_uptime", tpServerStatus, TpServiceStatus::getResponseTime).tag("uri",domain).register(registry);
                } else {
                    tpServerStatus = tpServerStatusMap.get(domain);
                }
            }
        }
        return tpServerStatus;
    }

    /**
     * 验证第三方服务状态
     * @param domain
     * @param tpServiceStatus
     * @param wasteTime
     */
    private void checkTpServiceStatus(String domain, TpServiceStatus tpServiceStatus, long wasteTime) {

        int cnt = 0;
        long responseTime = 0;
        long recordStartTime = 0;
        /*
         * 计数
         */
        synchronized (tpServiceStatus) {
            tpServiceStatus.setCount(tpServiceStatus.getCount() + 1);
            tpServiceStatus.setTotalResponseTime(tpServiceStatus.getTotalResponseTime() + wasteTime);

            cnt = tpServiceStatus.getCount();
            recordStartTime = tpServiceStatus.getRecordStartTime();
            responseTime = tpServiceStatus.getTotalResponseTime();
        }

        this.serviceStatusLoger.info("[" + domain + "]" + " count:[" + cnt + "] recordStartTime:["
                + TimeUtil.timestamp2date(recordStartTime) + "] totalResponseTime:[" + responseTime + "]");

        if ((cnt >= INTERFACE_TOTAL_COUNT)
                || ((System.currentTimeMillis() - recordStartTime) > INTERFACE_RECORD_COUNT_RESET_TIME)) {// 请求次数达到计数上线
            if ((cnt >= INTERFACE_TOTAL_COUNT)) {
                long avgTime = responseTime / cnt;
                if ((avgTime) > INTERFACE_RESPONSE_TIME) {// 平均响应时间
                    // 解决并发量大下瞬时间熔断失效问题
                    synchronized (tpServiceStatus) {
                        tpServiceStatus.setCloseTime(System.currentTimeMillis());// 关闭（更新关闭时间）
                    }
                    this.apiSwitchMonitor.info("[" + domain + "]" + " close time:["
                            + TimeUtil.timestamp2date(System.currentTimeMillis()) + "]" + "average response time:["
                            + avgTime + "]");
                }
            }

            synchronized (tpServiceStatus) {
                tpServiceStatus.setCount(0);
                tpServiceStatus.setTotalResponseTime(0);
                tpServiceStatus.setRecordStartTime(System.currentTimeMillis());
            }
        }
        //tpServerStatusMap.put(domain, tpServiceStatus);
    }

    /**
     * Fill the missing keys in the map
     * @param url
     *            url template
     * @param urlVariables
     *            variables map of url parameters
     */
    private void fillUrlVariables(String url, Map<String, ?> urlVariables) {
        if (urlVariables == null) {
            logger.warn("Variable map is null for URL:" + url);
            return;
        }

        Pattern pattern = Pattern.compile("\\{([^/]+?)\\}");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key != null && urlVariables.get(key) == null) {
                urlVariables.put(key, null);
            }
        }
    }

    public static String[] exUrlencodeTable = null;
    public static String exUrlEncode(final String sourceStr) {
        if (null == exUrlencodeTable) {
            exUrlencodeTable = new String[256];
            for (int i = 0; i < 256; i++) {
                if (i >= '0' && i <= '9' || i >= 'a' && i <= 'z' || i >= 'A' && i <= 'Z' || i == '-' || i == '_'
                        || i == '.' || i == ':' /*|| i == '{' || i == '}'*/) {
                    exUrlencodeTable[i] = (char) i + "";
                } else {
                    exUrlencodeTable[i] = "%" + String.format("%02x", i).toUpperCase();
                }
            }
        }
        final StringBuilder sb = new StringBuilder();
        try {
            int index = 0;
            for (int i = 0, length = sourceStr.length(); i < length; i++) {
                index = sourceStr.charAt(i) & 0xFF;
                sb.append(exUrlencodeTable[index]);
            }
        }  catch (Exception e) {

        }
        return sb.toString();
    }

    public static Map<String, Object> getUrlParams(String param) {
        Map<String, Object> map = new HashMap<String, Object>(0);
        if (StringUtils.isBlank(param)) {
            return map;
        }
        String[] params = param.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (null != entry.getValue()) {
                sb.append(entry.getKey() + "=" + entry.getValue());
            } else {
                sb.append(entry.getKey() + "=");
            }
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }

    /**
     * url参数签名生成算法
     * @param  params 请求参数集，所有参数必须已转换为字符串类型
     * @param secret 签名密钥
     * @return 签名
     * @throws IOException
     */
    public static String genOdpParamsSignature(Map<String, String> params, String secret) throws IOException {
        // 先将参数以其参数名的字典序升序进行排序
        Map<String, String> sortedParams = new TreeMap<String, String>(params);
        Set<Map.Entry<String, String>> entrys = sortedParams.entrySet();

        // 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起
        StringBuilder basestring = new StringBuilder();
        for (Map.Entry<String, String> param : entrys) {
            //basestring.append(param.getKey()).append("=").append(param.getValue());
            basestring.append(param.getValue());
        }
        basestring.append(secret);

        // 使用MD5对待签名串求签
        byte[] bytes = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            bytes = md5.digest(basestring.toString().getBytes("UTF-8"));
        } catch (GeneralSecurityException ex) {
            throw new IOException(ex);
        }

        // 将MD5输出的二进制结果转换为小写的十六进制
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex);
        }
        return sign.toString();
//        return MD5Util.md5(basestring.toString());
    }

    public Map<String, String> multiHttpRequests(String[] urls, String type, Object data, Map<String, String> headers) {
        return this.multiHttpRequests(urls, type, data, headers, true);
    }

    /**
     * 支持并发打包请求处理
     * @param urls
     *            : 如果post请求多组时url一样，需要特殊处理，eg.加一个url参数唯一标志
     * @param type
     * @param data
     * @param headers
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> multiHttpRequests(String[] urls, String type, Object data, Map<String, String> headers,
                                                 final boolean enableDownService) {
        final Map<String, String> ret = new HashMap<String, String>();

        if (null != urls && urls.length > 0) {
            if (null == type) {
                type = "GET";
            } else {
                type = type.toUpperCase();
            }
            AbstractHttpEntity entity = null;
            if (null != data) {
                if (data instanceof String) {
                    entity = new StringEntity((String) data, Charset.forName("UTF-8"));
                } else if (data instanceof Map) {
                    List<NameValuePair> list = new ArrayList<NameValuePair>();
                    Iterator iterator = ((Map) data).entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, String> elem = (Map.Entry<String, String>) iterator.next();
                        list.add(new BasicNameValuePair(elem.getKey(), elem.getValue()));
                    }
                    if (list.size() > 0) {
                        entity = new UrlEncodedFormEntity(list, Charset.forName("UTF-8"));
                    }
                } else {
                    // TODO
                }
            }

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout((int) INTERFACE_RESPONSE_TIME)
                    .setSocketTimeout((int) INTERFACE_RESPONSE_TIME).setConnectTimeout((int) INTERFACE_RESPONSE_TIME)
                    .build();

            if (urls.length > 1) {
                // TODO:待调整合理的并发数!!!
                CloseableHttpAsyncClient closeableHttpAsyncClient = HttpAsyncClients.custom()
                        .setDefaultRequestConfig(requestConfig).setMaxConnTotal(urls.length)
                        .setMaxConnPerRoute(urls.length).build();
                try {
                    closeableHttpAsyncClient.start();

                    if ("POST".equals(type)) {
                        HttpPost[] requests = new HttpPost[urls.length];
                        for (int i = 0; i < urls.length; i++) {
                            requests[i] = new HttpPost(urls[i]);
                            if (null != entity) {
                                requests[i].setEntity(entity);
                            }
                        }

                        final CountDownLatch latch = new CountDownLatch(requests.length);

                        for (final HttpPost request : requests) {
                            /*
                             * 第三方服务可用，执行请求
                             */
                            final String domain = this.getUrlTemplate(request.getURI().toString());
                            final long stime = System.currentTimeMillis();
                            final TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

                            if (enableDownService
                                    && ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME)) {// 距离最近一次关闭不到设定的时间间隔，直接返回
                                handleMultiHttpResponses(request.getURI().toString(), null, ret, stime,
                                        System.currentTimeMillis() - stime);
                                continue;
                            }

                            closeableHttpAsyncClient.execute(request, new FutureCallback<HttpResponse>() {

                                public void completed(final HttpResponse response) {
                                    latch.countDown();
                                    long wasteTime = System.currentTimeMillis() - stime;
                                    try {
                                        handleMultiHttpResponses(request.getURI().toString(), response, ret, stime,
                                                wasteTime);
                                    } catch (Exception e) {
                                        String logInfo = "|500|0|" + +(System.currentTimeMillis() - stime) + "|1";
                                        logger.warn(request.getURI().toString() + logInfo);
                                        if (null != SessionCache.getSession()) {
                                            SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                        }
                                    }
                                    /*
                                     * 验证第三方服务状态
                                     */
                                    if (enableDownService) {
                                        checkTpServiceStatus(domain, tpServerStatus, wasteTime);
                                    }
                                }

                                public void failed(final Exception ex) {
                                    latch.countDown();
                                    long wasteTime = System.currentTimeMillis() - stime;
                                    String logInfo = "|500|0|" + wasteTime + "|1";
                                    logger.warn(request.getURI().toString() + logInfo);
                                    if (null != SessionCache.getSession()) {
                                        SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                    }

                                    /*
                                     * 验证第三方服务状态
                                     */
                                    if (enableDownService) {
                                        checkTpServiceStatus(domain, tpServerStatus, wasteTime);
                                    }
                                }

                                public void cancelled() {
                                    latch.countDown();
                                    String logInfo = "|500|0|" + (System.currentTimeMillis() - stime) + "|-1";
                                    logger.warn(request.getURI().toString() + logInfo);
                                    if (null != SessionCache.getSession()) {
                                        SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                    }
                                }

                            });
                        }
                        latch.await();
                    } else {
                        HttpGet[] requests = new HttpGet[urls.length];
                        for (int i = 0; i < urls.length; i++) {
                            requests[i] = new HttpGet(urls[i]);
                        }
                        final CountDownLatch latch = new CountDownLatch(requests.length);
                        for (final HttpGet request : requests) {
                            /*
                             * 第三方服务可用，执行请求
                             */
                            final String domain = this.getUrlTemplate(request.getURI().toString());
                            final long stime = System.currentTimeMillis();
                            final TpServiceStatus tpServerStatus = getTpServiceStatus(domain);

                            if (enableDownService
                                    && ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME)) {// 距离最近一次关闭不到设定的时间间隔，直接返回
                                handleMultiHttpResponses(request.getURI().toString(), null, ret, stime,
                                        System.currentTimeMillis() - stime);
                                continue;
                            }

                            closeableHttpAsyncClient.execute(request, new FutureCallback<HttpResponse>() {

                                public void completed(final HttpResponse response) {
                                    latch.countDown();
                                    long wasteTime = System.currentTimeMillis() - stime;
                                    try {
                                        handleMultiHttpResponses(request.getURI().toString(), response, ret, stime,
                                                wasteTime);
                                    } catch (Exception e) {
                                        String logInfo = "|500|0|" + wasteTime + "|1";
                                        logger.warn(request.getURI().toString() + logInfo);
                                        if (null != SessionCache.getSession()) {
                                            SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                        }
                                    }

                                    /*
                                     * 验证第三方服务状态
                                     */
                                    if (enableDownService) {
                                        checkTpServiceStatus(domain, tpServerStatus, wasteTime);
                                    }
                                }

                                public void failed(final Exception ex) {
                                    latch.countDown();
                                    long wasteTime = System.currentTimeMillis() - stime;
                                    String logInfo = "|500|0|" + wasteTime + "|1";
                                    logger.warn(request.getURI().toString() + logInfo);
                                    if (null != SessionCache.getSession()) {
                                        SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                    }

                                    /*
                                     * 验证第三方服务状态
                                     */
                                    if (enableDownService) {
                                        checkTpServiceStatus(domain, tpServerStatus, wasteTime);
                                    }
                                }

                                public void cancelled() {
                                    latch.countDown();
                                    String logInfo = "|500|0|" + (System.currentTimeMillis() - stime) + "|-1";
                                    logger.warn(request.getURI().toString() + logInfo);
                                    if (null != SessionCache.getSession()) {
                                        SessionCache.getSession().setResponse(request.getURI().toString(), "", logInfo);
                                    }
                                }

                            });
                        }
                        latch.await();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    CloseUtil.close(closeableHttpAsyncClient);
                }
            } else {
                /*
                 * 初始化第三方服务状态对象
                 */
                long stime = System.currentTimeMillis();
                long wasteTime = 0;
                String completeUrl = urls[0]; // 已经拼接好的完整url，用于日志
                String domain = this.getUrlTemplate(completeUrl);// this.getDomain(url);
                TpServiceStatus tpServerStatus = getTpServiceStatus(domain);
                /*
                 * 验证第三方服务是否可用
                 */
                if (enableDownService
                        && ((System.currentTimeMillis() - tpServerStatus.getCloseTime()) < INTERFACE_AUTO_NORMAL_TIME)) {// 距离最近一次关闭不到设定的时间间隔，直接返回
                    try {
                        handleMultiHttpResponses(completeUrl, null, ret, stime, System.currentTimeMillis() - stime);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return ret;
                }

                CloseableHttpClient httpClient = HttpClientBuilder.create()
                        .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                        .setRedirectStrategy(new DefaultRedirectStrategy())
                        .setDefaultCookieStore(new BasicCookieStore()).setDefaultRequestConfig(requestConfig).build();
                CloseableHttpResponse response = null;
                try {
                    if (type.equals("POST")) {
                        HttpPost request = new HttpPost(completeUrl);
                        if (null != entity) {
                            request.setEntity(entity);
                        }
                        if (null != headers) {
                            Set<String> keys = headers.keySet();
                            for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                                String key = (String) i.next();
                                request.addHeader(key, headers.get(key));
                            }
                        }
                        response = httpClient.execute(request);
                    } else {
                        HttpGet request = new HttpGet(completeUrl);
                        if (null != headers) {
                            Set<String> keys = headers.keySet();
                            for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                                String key = (String) i.next();
                                request.addHeader(key, headers.get(key));
                            }
                        }
                        response = httpClient.execute(request);
                    }
                    wasteTime = System.currentTimeMillis() - stime;
                    handleMultiHttpResponses(completeUrl, response, ret, stime, wasteTime);
                } catch (Exception e) {
                    // #debug
                    e.printStackTrace();
                    String logInfo = "|500|0|" + wasteTime + "|1";
                    logger.warn(completeUrl + logInfo);
                    if (null != SessionCache.getSession()) {
                        SessionCache.getSession().setResponse(completeUrl, "", logInfo);
                    }
                } finally {
                    CloseUtil.close(response);
                    CloseUtil.close(httpClient);
                }

                /*
                 * 验证第三方服务状态
                 */
                if (enableDownService) {
                    this.checkTpServiceStatus(domain, tpServerStatus, wasteTime);
                }
            }
        }
        return ret;
    }

    private void handleMultiHttpResponses(String url, final HttpResponse response, Map<String, String> ret, long stime,
                                          long wasteTime) throws Exception {
        if (response != null) {
            String result = null;
            try {
                if (response.getEntity() != null) {
                    result = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                }
                if (StringUtil.isNotBlank(result)) {
                    ret.put(url, result);
                    String logInfo = "|" + response.getStatusLine().getStatusCode() + "|" + result.getBytes().length
                            + "|" + wasteTime + "|0";
                    this.logger.warn(url + logInfo);
                    if (null != SessionCache.getSession()) {
                        SessionCache.getSession().setResponse(url.toString(), result, logInfo);
                    }
                } else {
                    String logInfo = "|100|0|" + wasteTime + "|0";
                    this.logger.warn(url + logInfo);
                    if (null != SessionCache.getSession()) {
                        SessionCache.getSession().setResponse(url.toString(), "", logInfo);
                    }
                }
            } catch (Exception e) {
                throw e;
            }
        } else { // handle the case of down service => 505
            ret.put(url, ""); // TODO
            String logInfo = "|" + 505 + "|" + 0 + "|" + wasteTime + "|0";
            this.logger.warn(url + logInfo);
            if (null != SessionCache.getSession()) {
                SessionCache.getSession().setResponse(url.toString(), "", logInfo);
            }
        }
    }
}
