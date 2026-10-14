package com.example.financialanalytics.config;

import com.example.financialanalytics.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
class SecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               AuthService authService,
                                               RateLimitService rateLimitService,
                                               ObjectMapper objectMapper,
                                               org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        var entryPoint = new ApiAuthenticationEntryPoint(objectMapper);
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                                "/api/v1/auth/google").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new SessionAuthenticationFilter(authService, entryPoint),
                        AnonymousAuthenticationFilter.class)
                .addFilterBefore(new ApiRateLimitFilter(rateLimitService, objectMapper),
                        SessionAuthenticationFilter.class);
        return http.build();
    }
}
