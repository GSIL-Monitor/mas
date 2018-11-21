package com.letv.mas.router.omp.controller;

import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @RequestMapping("/allApplications")
    public BaseResponseDto allApplications(@RequestParam int pageSize, @RequestParam int pageNum, String token) {
        return applicationService.allApplications(pageSize, pageNum);
    }

    @RequestMapping("/userApplications")
    public BaseResponseDto userApplications(@RequestParam int pageSize, @RequestParam int pageNum, @RequestParam String token) {
        return applicationService.userApplications(pageSize, pageNum, token);
    }

    @RequestMapping("/updateApplications")
    public BaseResponseDto updateApplications(String client_secret, String id, @RequestParam String client_name, @RequestParam String en_name, @RequestParam String user_id) {
        return applicationService.updateApplications(client_secret, id, client_name, en_name, user_id);
    }

    @RequestMapping("/startUseApplications")
    public BaseResponseDto startUseApplications(@RequestParam String id, @RequestParam String client_id, @RequestParam String user_id) {
        return applicationService.startUseApplications(id, client_id, user_id);
    }

    @RequestMapping("/stopUseApplications")
    public BaseResponseDto stopUseApplications(@RequestParam String id, @RequestParam String client_id, @RequestParam String user_id) {
        return applicationService.stopUseApplications(id, client_id, user_id);
    }

    @RequestMapping("/passApplications")
    public BaseResponseDto passApplications(String id, @RequestParam String client_id, @RequestParam String en_name, @RequestParam String user_id) {
        return applicationService.passApplications(id, client_id, en_name, user_id);
    }

    @RequestMapping("/deleteApplications")
    public BaseResponseDto deleteApplications(@RequestParam String user_id, @RequestParam String client_id) {
        return applicationService.deleteApplications(user_id, client_id);
    }

    @RequestMapping("/searchApp")
    public BaseResponseDto searchApp(String user_id,String value) {
        return applicationService.searchApp(user_id,value);
    }
}
