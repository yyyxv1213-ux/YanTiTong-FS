package com.ytt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 研题通·考研408刷题训练系统 后端启动类
 *
 * @author ytt
 */
@SpringBootApplication
@MapperScan("com.ytt.mapper")
public class YttServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YttServerApplication.class, args);
    }
}
