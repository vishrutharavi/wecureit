package com.wecureit.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.authorization.AuthorizationDecision;

@Configuration
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;

    public SecurityConfig(FirebaseAuthFilter firebaseAuthFilter) {
        this.firebaseAuthFilter = firebaseAuthFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/agent/ask", "/api/health", "/health", "/actuator/**").permitAll()
                
                // DEBUG: Log when doctor availability endpoints are accessed
                .requestMatchers(HttpMethod.GET, "/api/doctors/*/availability", "/api/doctors/*/locked-availabilities")
                    .access((authentication, context) -> {
                        String uri = context.getRequest().getRequestURI();
                        String method = context.getRequest().getMethod();
                        boolean isAuthenticated = authentication.get().isAuthenticated();
                        String authorities = authentication.get().getAuthorities().toString();
                        
                        System.out.println("=== SECURITY DEBUG ===");
                        System.out.println("URI: " + uri);
                        System.out.println("Method: " + method);
                        System.out.println("Authenticated: " + isAuthenticated);
                        System.out.println("Authorities: " + authorities);
                        System.out.println("Matched rule: GET /api/doctors/*/availability");
                        
                        boolean hasAccess = isAuthenticated;
                        System.out.println("Access granted: " + hasAccess);
                        System.out.println("=====================");
                        
                        return new AuthorizationDecision(hasAccess);
                    })
                
                // Patient endpoints (including agent/booking for copilot)
                .requestMatchers("/api/patient/**", "/api/agent/**").hasRole("PATIENT")
                
                // Doctor endpoints (the GET availability is already handled above)
                .requestMatchers("/api/doctor/**", "/api/doctors/**")
                    .access((authentication, context) -> {
                        String uri = context.getRequest().getRequestURI();
                        System.out.println("=== DOCTOR ENDPOINT DEBUG ===");
                        System.out.println("URI: " + uri);
                        System.out.println("Matched rule: /api/doctor/** or /api/doctors/**");
                        System.out.println("Authorities: " + authentication.get().getAuthorities());
                        System.out.println("=============================");
                        
                        boolean hasRole = authentication.get().getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
                        return new AuthorizationDecision(hasRole);
                    })
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                firebaseAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // CORS is configured in `com.wecureit.config.CorsConfig`.
}
