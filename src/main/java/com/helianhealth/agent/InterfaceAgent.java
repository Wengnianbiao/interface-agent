package com.helianhealth.agent;

import tk.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.helianhealth.agent")
public class InterfaceAgent {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(InterfaceAgent.class, args);
    }
}
