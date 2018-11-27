package com.letv.mas.router.omp.service;

import com.letv.mas.router.omp.model.dto.BaseResponseDto;
import com.letv.mas.router.omp.util.HttpUtil;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class RequestForwardService {

    private HttpUtil httpUtil = new HttpUtil();

    public BaseResponseDto area() {
        String url = "http://10.110.140.77:8081/chart/h";
        BaseResponseDto response = new BaseResponseDto();
        String get = httpUtil.sendGet(url, "id=21284856&start=1542680100&end=1542814260&step=60&cf=");
        String substring = get.substring(get.indexOf("\"data\": [") + 9, get.indexOf("]]") + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        while(substring.length() > 0){
            String s = substring.substring(substring.indexOf("["), substring.indexOf("]") + 1);
            substring = substring.substring(substring.indexOf("]") + 1);
            String[] split = s.split(",");
            String s1 = "{X : '"+simpleDateFormat.format(Long.parseLong(split[0].substring(1)));
            String s2 = "',Y :"+split[1].substring(0,split[1].length() - 1) + "},";
            sb.append(s1 + s2);
        }
        sb.delete(sb.toString().length() - 1, sb.toString().length());
        sb.append("]");
        System.out.println(sb.toString());
        response.setData(sb.toString());
        return response;
    }
}
