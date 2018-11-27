package com.letv.mas.router.omp.service;

import com.letv.mas.router.omp.constant.StatusCode;
import com.letv.mas.router.omp.model.dao.OauthTokenMapper;
import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.util.JwtTokenUtil;
import com.letv.mas.router.omp.util.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;

@Service
public class OauthTokenService {

    @Resource
    private OauthTokenMapper oauthTokenMapper;

    private JwtTokenUtil util = new JwtTokenUtil();

    public BaseResponseDto refreshUseApplication(String user_id, String client_id,String client_secret,String access_token) {
        BaseResponseDto response = new BaseResponseDto();
        String refreshToken = null;
        if(StringUtils.isNotBlank(client_secret)){
            refreshToken = util.refreshToken("Bearer "+access_token,client_secret);
            if(refreshToken == null){
                refreshToken = util.genToken((System.currentTimeMillis() + 1 * 60 * 1000)+"",client_secret);
            }
        }else {
            refreshToken = util.refreshToken("Bearer "+access_token, StatusCode.CLIENT_SECRET_CODE);
            if(refreshToken == null){
                refreshToken = util.genToken((System.currentTimeMillis() + 1 * 60 * 1000)+"");
            }
        }
        Timestamp expires = new Timestamp(System.currentTimeMillis());
        int i = oauthTokenMapper.refreshUseApplication(user_id,client_id,refreshToken,refreshToken,expires);
        setResponseStatus(response,i);
        return response;
    }

    private void setResponseStatus(BaseResponseDto response, int i) {
        if (i != 0) {
            response.setStatus(1);
        } else {
            response.setStatus(0);
        }
    }
}
