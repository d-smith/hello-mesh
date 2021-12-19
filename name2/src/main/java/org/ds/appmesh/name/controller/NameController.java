package org.ds.appmesh.name.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NameController {
    private final static Logger logger = LoggerFactory.getLogger(NameController.class);

    private String[] names = {
            "Liam", "Noah", "Oliver", "Elijah", "Lucas",
            "Olivia", "Emma", "Amelia", "Ava", "Sophie"
    };

    @GetMapping("/name")
    public ResponseEntity<String> greeting() {
        int idx = (int) (Math.random() * names.length);
        return new ResponseEntity<>(names[idx], HttpStatus.OK);
    }
}
