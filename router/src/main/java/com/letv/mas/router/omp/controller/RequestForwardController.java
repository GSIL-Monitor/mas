package com.letv.mas.router.omp.controller;

import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.service.RequestForwardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/morris")
@RestController
public class RequestForwardController {

    @Autowired
    private RequestForwardService requestForwardService;

    @RequestMapping("/area")
    public BaseResponseDto area(){
        return requestForwardService.area();
    }

}
