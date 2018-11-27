package com.letv.mas.router.omp.model.dao;

import com.letv.mas.router.omp.model.bean.xdo.OauthTokenDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface OauthTokenMapper {

    int insertNewToken(OauthTokenDo auth);

    int stopUseApplications(@Param("client_id") String client_id, @Param("user_id") String user_id);

    int startUseApplications(@Param("client_id") String client_id, @Param("user_id") String user_id, @Param("token") String token, @Param("refresh_token") String refresh_token, @Param("expires") Timestamp expires);

    List<OauthTokenDo> userApplicationsToken(@Param("user_id") String token);

    int refreshUseApplication(@Param("user_id") String user_id, @Param("client_id") String client_id, @Param("refresh_token") String refreshToken,@Param("access_token") String token,@Param("expires") Timestamp expires);

    List<OauthTokenDo> findAllUserAccessToken();
}
