package com.pzhgp.backend.config;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final BreederRepository breederRepository;
    private final AuthCookieService authCookieService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String jwt = authCookieService.readToken(request);
        String userEmail = null;

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            userEmail = jwtService.extractEmail(jwt);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Odrzucono nieprawidłowy token JWT: {}", e.getMessage());
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<Breeder> breederOpt = breederRepository.findByEmail(userEmail);

            if (breederOpt.isPresent() && breederOpt.get().getStatus() == AccountStatus.ACTIVE) {
                String dbRole = breederOpt.get().getRole().name();
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(dbRole);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userEmail, null, Collections.singletonList(authority));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // Adres e-mail to dane osobowe, dlatego w logu jest tylko ID konta.
                log.warn("Odrzucono token: konto {} nie istnieje lub jest nieaktywne.", breederOpt.map(breeder -> "o ID " + breeder.getId()).orElse("z tokenu"));
            }
        }

        filterChain.doFilter(request, response);
    }
}