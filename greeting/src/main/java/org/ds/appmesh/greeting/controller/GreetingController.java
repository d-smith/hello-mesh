package org.ds.appmesh.greeting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
    private final static Logger logger = LoggerFactory.getLogger(GreetingController.class);

    private String[] greetings = {
            "Hi", "Hello", "Yo", "Sup", "Howdy", "Ok"
    };

    @GetMapping("/greeting")
    public ResponseEntity<String> greeting() {
        int idx = (int) (Math.random() * greetings.length);
        return new ResponseEntity<>(greetings[idx], HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("health endpoint called");
        return new ResponseEntity<>("Up", HttpStatus.OK);
    }
}
