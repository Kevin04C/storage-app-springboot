package com.stonestorage.bootstrap.config.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ApiKeyReactiveAuthenticationManager authenticationManager,
            ApiKeyAuthenticationConverter authenticationConverter,
            ObjectMapper objectMapper
        ) {
        AuthenticationWebFilter apiKeyAuthFilter = new AuthenticationWebFilter(authenticationManager);
        apiKeyAuthFilter.setServerAuthenticationConverter(authenticationConverter);
        apiKeyAuthFilter.setAuthenticationFailureHandler(authenticationFailureHandler(objectMapper));
        apiKeyAuthFilter.setRequiresAuthenticationMatcher(exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();
            boolean isPublic =
                    ((method == HttpMethod.GET || method == HttpMethod.HEAD) && (
                            path.startsWith("/swagger-ui") ||
                            path.startsWith("/v3/api-docs") ||
                            path.startsWith("/webjars") ||
                            path.startsWith("/f/") ||
                            path.equals("/health") ||
                            path.startsWith("/actuator/")
                    )) ||
                    (method == HttpMethod.POST && path.equals("/api/v1/client/register"));
            if (isPublic) {
                return ServerWebExchangeMatcher.MatchResult.notMatch();
            }
            return ServerWebExchangeMatcher.MatchResult.match();
        });

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/f/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/client/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/health", "/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(apiKeyAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(new ClientContextFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                )
                .build();
    }

    private ServerAuthenticationFailureHandler authenticationFailureHandler(ObjectMapper objectMapper) {
        return (exchange, ex) -> writeUnauthorized(exchange.getExchange(), objectMapper, ex.getMessage());
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, ex) -> writeUnauthorized(exchange, objectMapper, ex.getMessage());
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, ObjectMapper objectMapper, String details) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                details != null ? details : "Authentication required");

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            bytes = "{\"success\":false,\"message\":\"UNAUTHORIZED\",\"data\":null,\"error\":{\"code\":\"UNAUTHORIZED\",\"details\":\"Authentication required\"}}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
