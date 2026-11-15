package org.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Filter toàn cục của gateway:
 *  - Gán X-Request-Id cho mọi request đi vào (nếu client chưa gửi), chuyển tiếp xuống service phía sau
 *    và trả về cho client -> truy vết 1 request qua gateway, transaction-service, account-service.
 *  - Ghi log request được định tuyến tới route nào, URL đích nào và status trả về.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString().substring(0, 8) : incoming;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, requestId))
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();
        // set (không phải add) ngay trước khi gửi response, để không bị trùng với header service phía sau trả về
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(HEADER, requestId);
            return Mono.empty();
        });

        long start = System.currentTimeMillis();
        return chain.filter(mutated).then(Mono.fromRunnable(() -> {
            Route route = mutated.getAttribute(GATEWAY_ROUTE_ATTR);
            URI target = mutated.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            log.info("[api-gateway] rid={} {} {} -> route={} -> {} | status={} ({} ms)",
                    requestId, request.getMethod(), request.getURI().getRawPath(),
                    route != null ? route.getId() : "KHÔNG KHỚP",
                    target != null ? target : "-",
                    mutated.getResponse().getStatusCode(),
                    System.currentTimeMillis() - start);
        }));
    }

    /** Chạy đầu tiên để mọi filter phía sau và service đích đều nhận được X-Request-Id. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
