package com.letv.mas.router.omp.model.dao;

import com.letv.mas.router.omp.model.bean.xdo.UserDo;
import com.letv.mas.router.omp.model.dto.AclDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SSOLoginMapper {

    UserDo findUserByMail(@Param("mail") String mail);

    List<AclDto> findAllAcls();

    void insertUser(@Param("mail") String loginUser, @Param("user_id") String userId);

    List<UserDo> findAllUsers();
}
