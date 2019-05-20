package hu.klavorar.recommendationapi.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Component
public class SessionWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        WebSession session = exchange.getSession().block();
        if(!session.isStarted()) {
            session.start();
        }
        if(session.isExpired()) {
            session.invalidate();
        }
        return chain.filter(exchange);
    }
}
