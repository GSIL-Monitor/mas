package com.letv.mas.router.omp.service;

import com.letv.mas.router.omp.constant.StatusCode;
import com.letv.mas.router.omp.model.bean.xdo.ClientDo;
import com.letv.mas.router.omp.model.bean.xdo.OauthTokenDo;
import com.letv.mas.router.omp.model.bean.xdo.UserDo;
import com.letv.mas.router.omp.model.dao.ApplicationMapper;
import com.letv.mas.router.omp.model.dao.OauthTokenMapper;
import com.letv.mas.router.omp.model.dao.SSOLoginMapper;
import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.model.dto.ClientDto;
import com.letv.mas.router.omp.util.JwtTokenUtil;
import com.letv.mas.router.omp.util.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;

@Service
public class ApplicationService {

    @Resource
    private ApplicationMapper applicationMapper;

    @Resource
    private SSOLoginMapper ssoLoginMapper;

    @Resource
    private OauthTokenMapper oauthTokenMapper;

    final Base64.Decoder decoder = Base64.getDecoder();
    final Base64.Encoder encoder = Base64.getEncoder();

    public BaseResponseDto userApplications(int pageSize, int pageNum, String token) {
        BaseResponseDto response = new BaseResponseDto();
        try {
            token = new String(decoder.decode(token), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ClientDo> clientDtos = applicationMapper.userApplications(pageSize * (pageNum - 1), pageSize, token);
        if (clientDtos != null) {
            response.setStatus(1);
            if (clientDtos.size() != 0) {
                response.setData(clientDtos);
            }
        } else {
            response.setStatus(0);
        }
        System.out.println(response.getData());
        return response;
    }

    public BaseResponseDto updateApplications(String client_secret, String id, String client_name, String en_name, String user_id) {
        BaseResponseDto response = new BaseResponseDto();
        try {
            user_id = new String(decoder.decode(user_id), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isBlank(client_secret)) {
            client_secret = StatusCode.CLIENT_SECRET_CODE;
        }
        if (StringUtils.isNotBlank(id)) {
            long l = System.currentTimeMillis();
            Timestamp update_time = new Timestamp(l);
            if (checkClientNameRepeat(id, en_name, user_id, response)) return response;
            String client_id = MD5Util.md5(user_id + en_name);
            int i = applicationMapper.updateApplciation(id, client_name, en_name, client_id, client_secret, update_time, 1);
            if (i != 0) {
                response.setStatus(1);
            }
        } else {
            if (checkClientNameRepeat(en_name, user_id, response)) return response;
            String client_id = MD5Util.md5(user_id + en_name);
            int i = applicationMapper.insertApplciation(client_name, en_name, user_id, client_id, client_secret);
            if (i != 0) {
                response.setStatus(1);
            }
        }
        return response;
    }

    private boolean checkClientNameRepeat(String id, String en_name, String user_id, BaseResponseDto response) {
        ClientDo clientDo = applicationMapper.checkClientNameRepeat(id);
        List<ClientDo> clientDos = applicationMapper.userApplications(0, 20, user_id);
        if (clientDo != null && clientDos != null && clientDos.size() != 0) {
            for (ClientDo client : clientDos) {
                if (client.getEn_name().equals(en_name)) {
                    if (clientDo.getId().equals(client.getId())) {
                        return false;
                    } else if (!clientDo.getId().equals(client.getId()) && client.getFlag() == 1) {
                        return false;
                    } else {
                        response.setStatus(0);
                        response.setCode(StatusCode.CLIENT_NAME_REPEAT);
                        response.setMsg(StatusCode.CLIENT_NAME_REPEAT_MSG);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkClientNameRepeat(String en_name, String user_id, BaseResponseDto response) {
        List<ClientDo> clientDos = applicationMapper.userApplications(0, 20, user_id);
        if (clientDos != null && clientDos.size() != 0) {
            for (ClientDo client : clientDos) {
                if (client.getEn_name().equals(en_name)) {
                    response.setStatus(0);
                    response.setCode(StatusCode.CLIENT_NAME_REPEAT);
                    response.setMsg(StatusCode.CLIENT_NAME_REPEAT_MSG);
                    return true;
                }
            }
        }
        return false;
    }

    public BaseResponseDto allApplications(int pageSize, int pageNum) {
        BaseResponseDto response = new BaseResponseDto();
        List<ClientDto> clientDtos = applicationMapper.allApplications(pageSize * (pageNum - 1), pageSize);
        replaceUserName(response, clientDtos);
        return response;
    }

    private void replaceUserName(BaseResponseDto response, List<ClientDto> clientDtos) {
        List<UserDo> allUsers = ssoLoginMapper.findAllUsers();
        if (clientDtos != null && clientDtos.size() != 0 && allUsers != null && allUsers.size() != 0) {
            for (int i = 0; i < clientDtos.size(); i++) {
                for (int j = 0; j < allUsers.size(); j++) {
                    if (clientDtos.get(i).getUser_id().equals(allUsers.get(j).getUser_id())) {
                        clientDtos.get(i).setUser(allUsers.get(j).getMail());
                    }
                }
            }
            response.setData(clientDtos);
            response.setStatus(1);
        } else {
            response.setStatus(0);
        }
    }

    public BaseResponseDto startUseApplications(String id, String client_id, String user_id) {
        BaseResponseDto response = new BaseResponseDto();
        Long appCode = System.currentTimeMillis() + 1 * 60 * 1000;
        JwtTokenUtil util = new JwtTokenUtil();
        String token = util.genToken(appCode + "");
        String refresh_token = util.refreshToken(token);
        Timestamp expires = new Timestamp(appCode);
        int i = oauthTokenMapper.startUseApplications(client_id, user_id, token, refresh_token, expires);
        int j = applicationMapper.startUseApplications(id);
        setResponseStatus2(response, i, j);
        return response;
    }

    public BaseResponseDto passApplications(String id, String client_id, String en_name, String user_id) {
        BaseResponseDto response = new BaseResponseDto();
        OauthTokenDo auth = new OauthTokenDo();
        auth.setClient_id(client_id);
        auth.setUser_id(user_id);
        auth.setScope(en_name);
        Long appCode = System.currentTimeMillis() + 1 * 60 * 1000;
        JwtTokenUtil util = new JwtTokenUtil();
        String token = util.genToken(appCode + "");
        auth.setAccess_token(token);
        String refresh_token = util.refreshToken(token);
        auth.setRefresh_token(refresh_token);
        Timestamp expires = new Timestamp(appCode);
        auth.setExpires(expires);
        int i = oauthTokenMapper.insertNewToken(auth);
        int j = applicationMapper.passApplications(id);
        setResponseStatus2(response, i, j);
        return response;
    }

    private void setResponseStatus2(BaseResponseDto response, int i, int j) {
        if (i != 0 && j != 0) {
            response.setStatus(1);
        } else {
            response.setStatus(0);
        }
    }

    public BaseResponseDto stopUseApplications(String id, String client_id, String user_id) {
        BaseResponseDto response = new BaseResponseDto();
        int i = oauthTokenMapper.stopUseApplications(client_id, user_id);
        int j = applicationMapper.stopUseApplications(id);
        setResponseStatus2(response, i, j);
        return response;
    }

    private void setResponseStatus(BaseResponseDto response, int i) {
        if (i != 0) {
            response.setStatus(1);
        } else {
            response.setStatus(0);
        }
    }

    public BaseResponseDto deleteApplications(String user_id, String client_id) {
        BaseResponseDto response = new BaseResponseDto();
        int i = 0;
        try {
            i = applicationMapper.deleteApplications(user_id, client_id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setResponseStatus(response, i);
        return response;
    }

    public BaseResponseDto searchApp(String user_id,String value) {
        BaseResponseDto response = new BaseResponseDto();
        if(StringUtils.isNotBlank(user_id) && StringUtils.isNotBlank(value)){
            List<ClientDto> clientDtos = applicationMapper.searchApp(user_id,value);
            replaceUserName(response, clientDtos);
        }else if(StringUtils.isNotBlank(user_id)){
            List<ClientDto> clientDtos = applicationMapper.searchAppByUser_id(user_id);
            replaceUserName(response, clientDtos);
        }else if(StringUtils.isNotBlank(value)){
            List<ClientDto> clientDtos = applicationMapper.searchAppByClient_id(value);
            replaceUserName(response, clientDtos);
        }
        return response;
    }

}
