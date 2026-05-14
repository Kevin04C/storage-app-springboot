package com.stonestorage.bootstrap.config.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class ClientContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() instanceof ApiKeyAuthenticationToken token) {
                        var client = token.getClient();
                        exchange.getAttributes().put("clientId", client.getId());
                        exchange.getAttributes().put("clientBaseDir", client.getBaseDir());
                        exchange.getAttributes().put("clientQuotaBytes", client.getQuotaBytes());
                        exchange.getAttributes().put("clientUsedBytes", client.getUsedBytes());
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.empty())
                .then(chain.filter(exchange));
    }
}
