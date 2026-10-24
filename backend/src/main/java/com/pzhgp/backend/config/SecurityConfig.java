package com.pzhgp.backend.config;

import com.pzhgp.backend.entity.Role;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${cors.allowed-origins:http://localhost:3000}")
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
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(restAuthenticationEntryPoint).accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login-cookie", "/api/auth/logout", "/api/auth/csrf", "/api/sections").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/announcements").hasAnyAuthority(Role.ADMINISTRATOR.name(), Role.MODERATOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/announcements/**").hasAnyAuthority(Role.ADMINISTRATOR.name(), Role.MODERATOR.name())

                        .requestMatchers(HttpMethod.POST, "/api/forum/categories").hasAnyAuthority(Role.ADMINISTRATOR.name(), Role.MODERATOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/forum/categories/**").hasAnyAuthority(Role.ADMINISTRATOR.name(), Role.MODERATOR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/forum/categories/**").hasAuthority(Role.ADMINISTRATOR.name())

                        .requestMatchers(HttpMethod.POST, "/api/found-pigeons").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/board").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/board").hasAuthority(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/board/**").hasAuthority(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/board/**").hasAuthority(Role.ADMINISTRATOR.name())

                        .requestMatchers(HttpMethod.POST, "/api/flight-plans", "/api/flight-plans/**").hasAuthority(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/flight-plans/**").hasAuthority(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/flight-plans/**").hasAuthority(Role.ADMINISTRATOR.name())

                        .requestMatchers(HttpMethod.POST, "/api/flight-results/**").hasAuthority(Role.ADMINISTRATOR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/flight-results/**").hasAuthority(Role.ADMINISTRATOR.name())

                        .requestMatchers("/api/admin/**").hasAuthority(Role.ADMINISTRATOR.name())
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
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AuthCookieService.COOKIE_NAME.equals(cookie.getName())) {
                    return true;
                }
            }
        }

        // Żądania bez cookie uwierzytelniającego nie wykorzystują sesji użytkownika.
        return false;
    }
}