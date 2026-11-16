package com.securefindings.security;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrometheusEndpointStubController {

    public static final String ENDPOINT = "/actuator/prometheus";
    public static final String RESPONSE = "securefindings_test_metric 1\n";

    @GetMapping(value = ENDPOINT, produces = MediaType.TEXT_PLAIN_VALUE)
    public String scrape() {
        return RESPONSE;
    }
}