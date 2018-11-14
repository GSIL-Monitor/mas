package com.letv.mas.router.iptv.tvproxy.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by leeco on 18/10/15.
 */
@Component
public class JwtTokenUtil {

    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

    @Value("${security.jwt.token.expire-time:3600}")
    private long expireTime = 3600; // 1hr

    private static final String CLAIM_KEY_CODE = "code";
    private static final String CLAIM_KEY_DATE = "date";

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String genToken(String code) {
        return this.genToken(code, this.secretKey);
    }

    /**
     * 根据appCode生成签名
     *
     * @param code
     * @param code:  接入平台分配的secretKey
     * @return
     */
    public String genToken(String code, String secretKey) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime * 1000);
        Map<String, Object> claims = new HashMap<String, Object>();

        claims.put(CLAIM_KEY_CODE, code);
        claims.put(CLAIM_KEY_DATE, now);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public boolean validateToken(String token, String code) {
        return this.validateToken(token, code, this.secretKey);
    }

    /**
     * 根据token（可直接为http请求头里Authorization项带Bearer的值）及code进行验证
     *
     * @param token: 接入平台为app生成的签名
     * @param code:  接入平台分配的appCode
     * @param code:  接入平台分配的secretKey
     * @return
     */
    public boolean validateToken(String token, String code, String secretKey) {
        boolean ret = false;

        if (StringUtils.isBlank(token)) {
            return ret;
        } else {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7, token.length());
            }
        }
        Claims claims = this.getClaimsFromToken(token, secretKey);
        if (null != claims) {
            Date date = null;
            if (null != claims.getExpiration()) {
                date = claims.getExpiration();
            } else {
                date = new Date();
            }
            if (StringUtils.isNotBlank(code)) {
                ret = claims.get(CLAIM_KEY_CODE).equals(code) && date.after(new Date());
            } else {
                ret = date.after(new Date());
            }
        }

        return ret;
    }

    public String refreshToken(String token) {
        return this.refreshToken(token, this.secretKey);
    }

    /**
     * 根据token（可直接为http请求头里Authorization项带Bearer的值）刷新签名
     *
     * @param token: 接入平台为app生成的签名
     * @return
     */
    public String refreshToken(String token, String secretKey) {
        String ret = null;

        try {
            if (StringUtils.isBlank(token)) {
                return ret;
            } else {
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7, token.length());
                }
            }

            final Claims claims = getClaimsFromToken(token, secretKey);
            if (null != claims) {
                ret = this.genToken((String) claims.get(CLAIM_KEY_CODE), secretKey);
            } else {

            }
        } catch (Exception e) {
        }

        return ret;
    }

    private Claims getClaimsFromToken(String token, String secretKey) {
        Claims claims = null;

        try {
            claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            claims = null;
        }

        return claims;
    }
}
