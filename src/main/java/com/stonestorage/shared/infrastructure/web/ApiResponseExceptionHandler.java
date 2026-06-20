package com.stonestorage.shared.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stonestorage.client.domain.exception.ClientAlreadyExistsException;
import com.stonestorage.client.domain.exception.ClientNotFoundException;
import com.stonestorage.client.domain.exception.QuotaExceededException;
import com.stonestorage.shared.domain.exception.DomainException;
import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import com.stonestorage.storage.domain.exception.FileNotFoundException;
import com.stonestorage.storage.domain.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-2)
public class ApiResponseExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public ApiResponseExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_ERROR";
        String details = ex.getMessage();

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = status.name();
            details = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex instanceof ClientNotFoundException) {
            status = HttpStatus.UNAUTHORIZED;
            code = "UNAUTHORIZED";
        } else if (ex instanceof ClientAlreadyExistsException) {
            status = HttpStatus.CONFLICT;
            code = "CLIENT_ALREADY_EXISTS";
        } else if (ex instanceof QuotaExceededException) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            code = "QUOTA_EXCEEDED";
        } else if (ex instanceof FileNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = "FILE_NOT_FOUND";
        } else if (ex instanceof StorageException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "STORAGE_ERROR";
        } else if (ex instanceof DomainException) {
            status = HttpStatus.BAD_REQUEST;
            code = "DOMAIN_ERROR";
        } else if (ex instanceof NoResourceFoundException nre) {
            status = HttpStatus.valueOf(nre.getStatusCode().value());
            code = status.name();
        } else {
            log.error("Unexpected error: {}", ex.getMessage(), ex);
        }

        if (details == null || details.isBlank()) {
            details = status.getReasonPhrase();
        }

        ApiResponse<Void> response = ApiResponse.error(status, code, details);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + status.name() + "\",\"data\":null,\"error\":{\"code\":\"INTERNAL_ERROR\",\"details\":\"Failed to serialize error\"}}").getBytes(StandardCharsets.UTF_8);
        }

        var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
