package com.estela.neko;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class NekoApplication {

    private static final Logger logger = LoggerFactory.getLogger(NekoApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(NekoApplication.class, args);
        logger.info("Neko启动结束");

    }
}
