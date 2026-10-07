package com.getlinkdtd.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the API gateway application.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Constructor for framework use.
     */
    protected ApiGatewayApplication() {
    }

    /**
     * Starts the API gateway application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
