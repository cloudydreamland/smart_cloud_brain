package com.smartcloudbrain.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.smartcloudbrain.common.security.JwtService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtGatewayFilterTest {

  @Test
  void patientSiteConfigIsPublic() {
    JwtService jwtService = mock(JwtService.class);
    JwtGatewayFilter filter = new JwtGatewayFilter(jwtService);
    AtomicBoolean chained = new AtomicBoolean(false);
    GatewayFilterChain chain = exchange -> {
      chained.set(true);
      return Mono.empty();
    };
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/patient-site/config").build());

    filter.filter(exchange, chain).block();

    verify(jwtService, never()).verify(org.mockito.ArgumentMatchers.anyString());
    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
  }

  @Test
  void protectedPathsStillRequireToken() {
    JwtGatewayFilter filter = new JwtGatewayFilter(mock(JwtService.class));
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/admin/patient-site/config").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }
}
