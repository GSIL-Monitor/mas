package com.letv.mas.router.omp.model.bean.xdo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * Created by leeco on 18/10/19.
 * `id` bigint(20) NOT NULL auto_increment,
 * `client_name` varchar(100) NOT NULL COMMENT '应用名称',
 * `en_name` varchar(50) DEFAULT NULL COMMENT '应用英文名称',
 * `user_id` varchar(32) NOT NULL COMMENT '用户id',
 * `client_id` varchar(32) DEFAULT '' COMMENT '应用（业务）id',
 * `client_secret` varchar(32) DEFAULT '' COMMENT '应用密钥',
 * `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 * `update_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '修改时间',
 * `end_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '终止使用时间',
 * `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '应用状态 (0：填报资料  1：送审中  2：审核失败  3：审核成功)',
 * `cause` varchar(200) DEFAULT NULL COMMENT '驳回原因',
 * `desc` varchar(200) DEFAULT NULL COMMENT '简介',
 * `flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否启用(0:启用，1:删除，2:停用)',
 */
@Data
public class ClientDo {
    public Long id;
    public String user_id;
    public String login_type;
    public String client_name;
    public String en_name;
    public String client_id;
    public String client_secret;
    public Timestamp create_time;
    public Timestamp update_time;
    public Timestamp end_time;
    public int status;
    public String cause;
    public String desc;
    public int flag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getLogin_type() {
        return login_type;
    }

    public void setLogin_type(String login_type) {
        this.login_type = login_type;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getEn_name() {
        return en_name;
    }

    public void setEn_name(String en_name) {
        this.en_name = en_name;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    public Timestamp getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Timestamp create_time) {
        this.create_time = create_time;
    }

    public Timestamp getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Timestamp update_time) {
        this.update_time = update_time;
    }

    public Timestamp getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Timestamp end_time) {
        this.end_time = end_time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
