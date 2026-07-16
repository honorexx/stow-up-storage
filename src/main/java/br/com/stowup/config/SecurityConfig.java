package br.com.stowup.config;

import br.com.stowup.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/index.html",
                        "/styles.css",
                        "/app.js",
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/assets/**",
                        "/uploads/profiles/**",
                        "/uploads/spaces/**",
                        "/staff",
                        "/staff/**")
                    .permitAll()
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/payments/webhooks/mercado-pago")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/legal/**", "/legal/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/spaces", "/api/spaces/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/bookings/availability/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/owner/spaces")
                    .authenticated()
                    .requestMatchers(
                        HttpMethod.POST, "/api/owner/spaces", "/api/owner/spaces/*/photos")
                    .authenticated()
                    .requestMatchers(
                        "/api/owner/spaces/*/documents/**", "/api/owner/spaces/*/availability")
                    .authenticated()
                    .requestMatchers("/api/staff/support/**")
                    .hasAnyRole("SUPPORT", "ADMIN")
                    .requestMatchers("/api/staff/moderation/**")
                    .hasAnyRole("LISTING_ANALYST", "MODERATOR", "ADMIN")
                    .requestMatchers("/api/staff/contracts/**")
                    .hasAnyRole("CONTRACT_ANALYST", "MODERATOR", "ADMIN")
                    .requestMatchers("/api/staff/listing-documents/**")
                    .hasAnyRole("DOCUMENT_ANALYST", "ADMIN")
                    .requestMatchers("/api/staff/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/support/**")
                    .hasAnyRole("CUSTOMER", "OWNER", "ADMIN")
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/owner/**")
                    .hasAnyRole("OWNER", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
