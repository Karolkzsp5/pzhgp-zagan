package com.pzhgp.backend.config;

import com.pzhgp.backend.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpMethod.*;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMINISTRATOR = Role.ADMINISTRATOR.name();
    private static final String MODERATOR = Role.MODERATOR.name();

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final AuthCookieService authCookieService;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.spa()
                        .requireCsrfProtectionMatcher(this::requiresCsrfProtection))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login-cookie", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/sections").permitAll()
                        .requestMatchers(GET, "/api/announcements").permitAll()
                        .requestMatchers(POST, "/api/announcements").hasAnyAuthority(ADMINISTRATOR, MODERATOR)
                        .requestMatchers(PUT, "/api/announcements/**").hasAnyAuthority(ADMINISTRATOR, MODERATOR)

                        .requestMatchers(POST, "/api/forum/categories").hasAnyAuthority(ADMINISTRATOR, MODERATOR)
                        .requestMatchers(PUT, "/api/forum/categories/**").hasAnyAuthority(ADMINISTRATOR, MODERATOR)
                        .requestMatchers(DELETE, "/api/forum/categories/**").hasAuthority(ADMINISTRATOR)

                        .requestMatchers(POST, "/api/found-pigeons").permitAll()

                        .requestMatchers(GET, "/api/board").permitAll()
                        .requestMatchers(POST, "/api/board").hasAuthority(ADMINISTRATOR)
                        .requestMatchers(PUT, "/api/board/**").hasAuthority(ADMINISTRATOR)
                        .requestMatchers(DELETE, "/api/board/**").hasAuthority(ADMINISTRATOR)

                        .requestMatchers(POST, "/api/flight-plans", "/api/flight-plans/**").hasAuthority(ADMINISTRATOR)
                        .requestMatchers(PUT, "/api/flight-plans/**").hasAuthority(ADMINISTRATOR)
                        .requestMatchers(DELETE, "/api/flight-plans/**").hasAuthority(ADMINISTRATOR)

                        .requestMatchers(POST, "/api/flight-results/**").hasAuthority(ADMINISTRATOR)
                        .requestMatchers(DELETE, "/api/flight-results/**").hasAuthority(ADMINISTRATOR)

                        .requestMatchers("/api/admin/**").hasAuthority(ADMINISTRATOR)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private boolean requiresCsrfProtection(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method)
                || "OPTIONS".equals(method) || "TRACE".equals(method)) {
            return false;
        }

        // Te operacje zawsze wymagają tokenu CSRF, także przed zalogowaniem.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/api/auth/login-cookie".equals(path) || "/api/auth/logout".equals(path)) {
            return true;
        }

        // Wszystkie operacje z ciasteczkiem uwierzytelniającym wymagają CSRF.
        // Żądania bez tego ciasteczka nie wykorzystują sesji użytkownika.
        return authCookieService.hasAuthenticationCookie(request);
    }
}
