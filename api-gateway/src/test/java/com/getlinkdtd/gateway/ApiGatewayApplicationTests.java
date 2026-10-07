package com.getlinkdtd.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "AUTH_SERVICE_URL=http://auth-service.test:8081",
        "LINK_SERVICE_URL=http://link-service.test:8082"
})
class ApiGatewayApplicationTests {
    @Autowired
    RouteDefinitionLocator routes;

    @Test
    void routesAuthAndLinkApisToTheirServices() {
        Map<String, URI> configured = routes.getRouteDefinitions()
                .collectList()
                .blockOptional()
                .orElseThrow()
                .stream()
                .collect(Collectors.toMap(definition -> definition.getId(), definition -> definition.getUri()));

        assertThat(configured).containsEntry("auth-service", URI.create("http://auth-service.test:8081"));
        assertThat(configured).containsEntry("link-service", URI.create("http://link-service.test:8082"));
        assertThat(configured).hasSize(2);
    }
}
