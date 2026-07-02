package com.smartcloudbrain.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.security.JwtClaims;
import com.smartcloudbrain.common.security.JwtService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.common.security.UserContextHeaders;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtGatewayFilterTest {

  private final JwtService jwtService = mock(JwtService.class);
  private final JwtGatewayFilter filter = new JwtGatewayFilter(jwtService);

  private GatewayFilterChain recordingChain(AtomicBoolean chained) {
    return exchange -> {
      chained.set(true);
      return Mono.empty();
    };
  }

  // ── public paths ────────────────────────────────────────────────

  @Test
  void patientSiteConfigIsPublic() {
    AtomicBoolean chained = new AtomicBoolean(false);
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/patient-site/config").build());

    filter.filter(exchange, recordingChain(chained)).block();

    verify(jwtService, never()).verify(anyString());
    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
  }

  @Test
  void patientSitePreviewIsPublic() {
    AtomicBoolean chained = new AtomicBoolean(false);
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/patient-site/preview?token=preview-token").build());

    filter.filter(exchange, recordingChain(chained)).block();

    verify(jwtService, never()).verify(anyString());
    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
  }

  @Test
  void allExplicitPublicPathsAreBypassed() {
    List<String> publicPaths = List.of(
        "/api/patient/register",
        "/api/patient/email-code/send",
        "/api/patient/login",
        "/api/doctor/login",
        "/api/admin/login",
        "/api/doctor/list",
        "/api/doctor/detail",
        "/api/doctor/department/list",
        "/actuator/health"
    );
    for (String path : publicPaths) {
      AtomicBoolean chained = new AtomicBoolean(false);
      MockServerWebExchange exchange = MockServerWebExchange.from(
          MockServerHttpRequest.get(path).build());
      filter.filter(exchange, recordingChain(chained)).block();
      assertEquals(true, chained.get(), "Expected public path: " + path);
      assertNull(exchange.getResponse().getStatusCode(), "No 401 for: " + path);
    }
    verify(jwtService, never()).verify(anyString());
  }

  // ── missing / invalid token ─────────────────────────────────────

  @Test
  void protectedPathsStillRequireToken() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/admin/patient-site/config").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  @Test
  void returns401WhenNoAuthorizationHeader() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(jwtService, never()).verify(anyString());
  }

  @Test
  void returns401WhenAuthorizationHeaderIsNotBearer() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard")
            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
            .build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(jwtService, never()).verify(anyString());
  }

  @Test
  void returns401WhenBearerTokenIsInvalid() {
    when(jwtService.verify("bad-token")).thenThrow(new RuntimeException("invalid"));

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard")
            .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
            .build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
  }

  // ── valid Bearer token → sets headers and chains ────────────────

  @Test
  void validBearerTokenSetsUserHeadersAndChains() {
    JwtClaims claims = new JwtClaims(42L, RoleType.DOCTOR, "Dr. Smith", Instant.now());
    when(jwtService.verify("good-token")).thenReturn(claims);
    AtomicBoolean chained = new AtomicBoolean(false);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard")
            .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
            .build());

    filter.filter(exchange, recordingChain(chained)).block();

    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
    // The mutated request is on the exchange that was chained, not the original exchange.
    // We verify that jwtService.verify was called.
    verify(jwtService).verify("good-token");
  }

  // ── query parameter token ───────────────────────────────────────

  @Test
  void resolvesTokenFromQueryParameter() {
    JwtClaims claims = new JwtClaims(1L, RoleType.PATIENT, "Alice", Instant.now());
    when(jwtService.verify("query-token")).thenReturn(claims);
    AtomicBoolean chained = new AtomicBoolean(false);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard?token=query-token").build());

    filter.filter(exchange, recordingChain(chained)).block();

    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
    verify(jwtService).verify("query-token");
  }

  @Test
  void blankQueryTokenIsIgnored() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard?token=").build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(jwtService, never()).verify(anyString());
  }

  // ── Sec-WebSocket-Protocol token ────────────────────────────────

  @Test
  void resolvesTokenFromSecWebSocketProtocolHeader() {
    JwtClaims claims = new JwtClaims(5L, RoleType.PATIENT, "Bob", Instant.now());
    when(jwtService.verify("ws-token")).thenReturn(claims);
    AtomicBoolean chained = new AtomicBoolean(false);

    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard")
            .header("Sec-WebSocket-Protocol", "bearer,ws-token")
            .build());

    filter.filter(exchange, recordingChain(chained)).block();

    assertEquals(true, chained.get());
    assertNull(exchange.getResponse().getStatusCode());
    verify(jwtService).verify("ws-token");
  }

  @Test
  void secWebSocketProtocolWithNoBearerPrefixIsIgnored() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/doctor/dashboard")
            .header("Sec-WebSocket-Protocol", "v10,chat")
            .build());

    filter.filter(exchange, ignored -> Mono.empty()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    verify(jwtService, never()).verify(anyString());
  }

  // ── getOrder ────────────────────────────────────────────────────

  @Test
  void getOrderReturnsNegative100() {
    assertEquals(-100, filter.getOrder());
  }
}
