package com.letv.mas.router.omp.controller;

import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.service.OauthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class OauthTokenController {

    @Autowired
    private OauthTokenService oauthTokenService;

    @RequestMapping("/refreshUseApplication")
    public BaseResponseDto refreshUseApplication(@RequestParam String access_token,String client_secret,@RequestParam String user_id,@RequestParam String client_id){
        return oauthTokenService.refreshUseApplication(user_id,client_id,client_secret,access_token);
    }

}
