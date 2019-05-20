package hu.klavorar.recommendationapi.config;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
@EnableSpringWebSession
public class SessionConfig {

    public static final String SESSION_MAP_NAME = "SPRING_SESSIONS";

    @Autowired
    private HazelcastInstance hazelcast;

    @Bean
    public ReactiveSessionRepository sessionRepository() {
        return new ReactiveMapSessionRepository(hazelcast.getMap(SESSION_MAP_NAME));
    }

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        return new CookieWebSessionIdResolver();
    }

}
