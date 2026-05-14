package com.stonestorage.client.infrastructure.web;

import com.stonestorage.client.application.dto.ClientQuotaInfo;
import com.stonestorage.client.application.dto.ClientResponse;
import com.stonestorage.client.application.dto.RegisterClientRequest;
import com.stonestorage.client.application.port.in.GetClientQuotaUseCase;
import com.stonestorage.client.application.port.in.RegisterClientUseCase;
import com.stonestorage.shared.infrastructure.web.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Tag(name = "Cliente", description = "Operaciones de gestión de clientes y cuotas")
public class ClientController {

    private final GetClientQuotaUseCase getClientQuotaUseCase;
    private final RegisterClientUseCase registerClientUseCase;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo cliente", description = "Crea un cliente con API Key única y carpeta base en PascalCase. El campo quotaGb es opcional (default 20 GB).")
    public Mono<ApiResponse<ClientResponse>> register(@Valid @RequestBody RegisterClientRequest request) {
        return registerClientUseCase.register(request)
                .map(response -> ApiResponse.ok(response, "Client registered successfully"));
    }

    @GetMapping("/quota")
    @Operation(summary = "Obtener cuota del cliente", description = "Retorna información de cuota total, usada y disponible del cliente autenticado.")
    @SecurityRequirement(name = "ApiKeyAuth")
    public Mono<ApiResponse<ClientQuotaInfo>> getQuota(@RequestAttribute("clientId") UUID clientId) {
        return getClientQuotaUseCase.getQuota(clientId)
                .map(quota -> ApiResponse.ok(quota, "Quota retrieved successfully"));
    }
}
