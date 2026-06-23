package com.smartcloudbrain.gateway.security;

import com.smartcloudbrain.common.security.JwtClaims;
import com.smartcloudbrain.common.security.JwtService;
import com.smartcloudbrain.common.security.UserContextHeaders;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

  private static final List<String> PUBLIC_PATHS = List.of(
      "/api/patient/register",
      "/api/patient/login",
      "/api/doctor/login",
      "/api/admin/login",
      "/api/doctor/list",
      "/api/doctor/detail",
      "/api/doctor/department/list",
      "/api/patient-site/config",
      "/actuator/health"
  );

  private final JwtService jwtService;

  public JwtGatewayFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (isPublic(path)) {
      return chain.filter(exchange);
    }
    String token = resolveToken(exchange.getRequest());
    if (token == null) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
    try {
      JwtClaims claims = jwtService.verify(token);
      ServerHttpRequest request = exchange.getRequest().mutate()
          .headers(headers -> {
            headers.remove(UserContextHeaders.USER_ID);
            headers.remove(UserContextHeaders.USER_ROLE);
            headers.remove(UserContextHeaders.USER_NAME);
            headers.add(UserContextHeaders.USER_ID, claims.userId().toString());
            headers.add(UserContextHeaders.USER_ROLE, claims.role().name());
            headers.add(UserContextHeaders.USER_NAME, URLEncoder.encode(claims.name(), StandardCharsets.UTF_8));
          })
          .build();
      return chain.filter(exchange.mutate().request(request).build());
    } catch (RuntimeException ex) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
  }

  @Override
  public int getOrder() {
    return -100;
  }

  private boolean isPublic(String path) {
    return PUBLIC_PATHS.contains(path);
  }

  private String resolveToken(ServerHttpRequest request) {
    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authorization != null && authorization.startsWith("Bearer ")) {
      return authorization.substring("Bearer ".length());
    }
    String queryToken = request.getQueryParams().getFirst("token");
    if (queryToken != null && !queryToken.isBlank()) {
      return queryToken;
    }
    List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
    if (protocols != null) {
      for (String header : protocols) {
        String[] parts = header.split(",");
        for (int i = 0; i < parts.length - 1; i++) {
          if ("bearer".equalsIgnoreCase(parts[i].trim())) {
            return parts[i + 1].trim();
          }
        }
      }
    }
    return null;
  }
}

