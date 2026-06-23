package com.sylvester.apigateway.redis_and_rate_limiting;

import io.github.bucket4j.Bucket;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitingService rateLimitingService;

    @Override
    public Mono<Void> filter(
            @NotNull ServerWebExchange exchange,
            @NotNull GatewayFilterChain chain
    ) {
        ServerHttpRequest request = exchange.getRequest();

        if (!request.getURI().getPath().startsWith("/api")) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(request);

        Bucket tokenBucket = rateLimitingService.resolveBucket(clientIp);
        var probe = tokenBucket.tryConsumeAndReturnRemaining(1);

        ServerHttpResponse response = exchange.getResponse();

        if (probe.isConsumed()) {
            response.getHeaders().add(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );

            return chain.filter(exchange);
        }

        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(
                "X-Rate-Limit-Retry-After-Seconds",
                String.valueOf(waitForRefill)
        );
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = """
                {
                  "status": %d,
                  "error": "Too Many Requests",
                  "message": "You have exhausted your API request quota",
                  "retryAfterSeconds": %d
                }
                """.formatted(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                waitForRefill
        );

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes))
        );
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return -2;
    }
}