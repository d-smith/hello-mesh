package org.ds.appmesh.hello.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {
    private static Logger logger = LoggerFactory.getLogger(HelloController.class);

    private String NAME_ENDPOINT = System.getenv("NAME_ENDPOINT");
    private String GREETING_ENDPOINT = System.getenv("GREETING_ENDPOINT");

    private static RestTemplate rest  = new RestTemplate();
    private static HttpEntity<String> requestEntity = new HttpEntity<String>("");


    private static String getDataFromEndpoint(String endpoint, String path, String errDefault) {
        logger.info("get data from {}, path {}", endpoint, path);
        ResponseEntity<String> responseEntity = rest.exchange(endpoint + path, HttpMethod.GET, requestEntity, String.class);
        if(responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            logger.info("Non-ok staus returned - {}", responseEntity.getStatusCode());
            logger.info(responseEntity.getBody());
            return errDefault;
        }
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        logger.info("get on /hello...");
        String name = "default name";
        String greeting = "default greeting";

        try {
            name = getDataFromEndpoint(NAME_ENDPOINT, "/name", "some name");
        }
        catch (Throwable t) {
            logger.warn(t.getMessage());
        }
        logger.info("got name {}", name);

        try {
            greeting = getDataFromEndpoint(GREETING_ENDPOINT,"/greeting", "some greeting");
        } catch(Throwable t) {
            logger.warn(t.getMessage());
        }
        
        logger.info("got greeting {}", greeting);

        return new ResponseEntity<>(greeting + ", " + name, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("get on /health...");
        return new ResponseEntity<>("hello UP", HttpStatus.OK);
    }
}
