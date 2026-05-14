package com.stonestorage.storage.infrastructure.web;

import com.stonestorage.client.domain.exception.ClientAlreadyExistsException;
import com.stonestorage.client.domain.exception.ClientNotFoundException;
import com.stonestorage.client.domain.exception.QuotaExceededException;
import com.stonestorage.shared.domain.exception.DomainException;
import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import com.stonestorage.storage.domain.exception.FileNotFoundException;
import com.stonestorage.storage.domain.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientAlreadyExistsException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleClientAlreadyExists(ClientAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "CLIENT_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleClientNotFound(ClientNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(QuotaExceededException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleQuotaExceeded(QuotaExceededException ex) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "QUOTA_EXCEEDED", ex.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleFileNotFound(FileNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleStorage(StorageException ex) {
        log.error("Storage error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleDomain(DomainException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "DOMAIN_ERROR", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGeneric(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "INTERNAL_ERROR";
        String details = ex.getMessage();

        if (ex instanceof NoResourceFoundException nre) {
            status = HttpStatus.valueOf(nre.getStatusCode().value());
            code = status.name();
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            code = status.name();
            details = rse.getReason();
        } else {
            log.error("Unexpected error: {}", ex.getMessage(), ex);
        }

        if (details == null || details.isBlank()) {
            details = status.getReasonPhrase();
        }

        return buildResponse(status, code, details);
    }

    private Mono<ResponseEntity<ApiResponse<Void>>> buildResponse(HttpStatus status, String code, String details) {
        return Mono.just(ResponseEntity.status(status).body(
                ApiResponse.<Void>error(status, code, details)
        ));
    }
}
