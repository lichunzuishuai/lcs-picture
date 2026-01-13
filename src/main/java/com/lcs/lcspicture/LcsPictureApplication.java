package com.lcs.lcspicture;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.lcs.lcspicture.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class LcsPictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(LcsPictureApplication.class, args);
    }

}
