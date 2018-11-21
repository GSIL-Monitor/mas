package com.letv.mas.router.omp;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class ServiceOmpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOmpApplication.class, args);
    }

}
