package com.sylvester.apigateway.config;



import com.sylvester.apigateway.redis_and_rate_limiting.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws Exception {

        return http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("http://localhost:5174", "http://localhost:5173"));
                    corsConfig.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setAllowedHeaders(List.of("*"));
                    return corsConfig;
                }))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> {
                    auth
                            .pathMatchers("/api/v1/auth/public/**").permitAll()
                            .pathMatchers("/actuator/**").permitAll()
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(Customizer.withDefaults());
                })
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .build();
    }


}
