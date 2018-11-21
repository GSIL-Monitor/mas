package com.letv.mas.router.omp.model.dao;

import com.letv.mas.router.omp.model.bean.xdo.ClientDo;
import com.letv.mas.router.omp.model.dto.ClientDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ApplicationMapper {
    List<ClientDo> userApplications(@Param("startIndex") int i, @Param("size") int pageSize, @Param("token") String token);

    int updateApplciation(@Param("id") String id, @Param("client_name") String client_name, @Param("en_name") String en_name, @Param("client_id") String client_id, @Param("client_secret") String client_secret, @Param("update_time") Timestamp update_time, @Param("status") int i);

    int insertApplciation(@Param("client_name") String client_name, @Param("en_name") String en_name, @Param("user_id") String user_id, @Param("client_id") String client_id, @Param("client_secret") String client_secret);

    List<ClientDto> allApplications(@Param("startIndex") int i, @Param("size") int pageSize);

    int deleteApplications(@Param("user_id") String user_id, @Param("client_id") String client_id);

    int stopUseApplications(@Param("id") String id);

    int startUseApplications(@Param("id") String id);

    int passApplications(@Param("id") String id);

    List<ClientDto> searchApp(@Param("user_id") String user_id,@Param("value") String value);

    List<ClientDto> searchAppByUser_id(@Param("user_id") String user_id);

    List<ClientDto> searchAppByClient_id(@Param("value") String value);

    ClientDo checkClientNameRepeat(@Param("id") String id);
}

