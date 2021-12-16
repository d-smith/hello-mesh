package org.ds.appmesh.greeting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GreetingApplication {
    private final static Logger logger = LoggerFactory.getLogger(GreetingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GreetingApplication.class, args);
    }
}