package br.com.stowup.config;

import br.com.stowup.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    return http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
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

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*",
            "http://10.*:*",
            "http://172.16.*:*",
            "http://172.17.*:*",
            "http://172.18.*:*",
            "http://172.19.*:*",
            "http://172.2*.*:*",
            "http://172.30.*:*",
            "http://172.31.*:*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    configuration.setExposedHeaders(List.of("Content-Disposition"));
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
