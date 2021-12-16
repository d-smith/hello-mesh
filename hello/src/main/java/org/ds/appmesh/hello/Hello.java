package org.ds.appmesh.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Hello {
    private static Logger logger = LoggerFactory.getLogger(Hello.class);

    public static void main(String... args) {
/*
        String endpoint = System.getenv("ENDPOINT");
        logger.info("endpoint is {}", endpoint);
        RestTemplate rest  = new RestTemplate();
        HttpEntity<String> requestEntity = new HttpEntity<String>("");


        for(;;) {
            ResponseEntity<String> responseEntity = rest.exchange(endpoint + "/greeting", HttpMethod.GET, requestEntity, String.class);

            if(responseEntity.getStatusCode() == HttpStatus.OK) {
                logger.info(responseEntity.getBody());
            } else {
                logger.info("Non-ok staus returned - {}", responseEntity.getStatusCode());
                logger.info(responseEntity.getBody());
            }
            try {
                Thread.sleep(1000);
            } catch(Throwable t) {}
        }

 */
        SpringApplication.run(Hello.class, args);
    }

}
