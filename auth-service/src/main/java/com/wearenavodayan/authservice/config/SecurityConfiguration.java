package com.wearenavodayan.authservice.config;

import com.wearenavodayan.authservice.exception.SecurityErrorResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        SecurityErrorResponseWriter errorResponseWriter)
                        throws Exception {

                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh",
                                                                "/actuator/health",
                                                                "/actuator/health/**",
                                                                "/actuator/info")
                                                .permitAll()
                                                .requestMatchers("/api/v1/admin/**")
                                                .hasRole("ADMIN")
                                                .anyRequest()
                                                .authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response,
                                                                exception) -> errorResponseWriter.write(
                                                                                request,
                                                                                response,
                                                                                HttpStatus.UNAUTHORIZED.value(),
                                                                                "UNAUTHORIZED",
                                                                                "Authentication is required or the access token is invalid."))
                                                .accessDeniedHandler((request, response,
                                                                exception) -> errorResponseWriter.write(
                                                                                request,
                                                                                response,
                                                                                HttpStatus.FORBIDDEN.value(),
                                                                                "FORBIDDEN",
                                                                                "You do not have permission to access this resource.")))
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .authenticationEntryPoint((request, response,
                                                                exception) -> errorResponseWriter.write(
                                                                                request,
                                                                                response,
                                                                                HttpStatus.UNAUTHORIZED.value(),
                                                                                "UNAUTHORIZED",
                                                                                "Authentication is required or the access token is invalid."))
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter())))
                                .build();
        }

        @Bean
        Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

                authoritiesConverter.setAuthoritiesClaimName("role");
                authoritiesConverter.setAuthorityPrefix("ROLE_");

                JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

                authenticationConverter.setJwtGrantedAuthoritiesConverter(
                                authoritiesConverter);

                return authenticationConverter;
        }
}