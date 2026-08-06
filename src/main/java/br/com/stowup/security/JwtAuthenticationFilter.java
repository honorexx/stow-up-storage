package br.com.stowup.security;

import br.com.stowup.account.application.port.out.AccountStore;
import br.com.stowup.account.domain.Account;
import br.com.stowup.account.domain.AccountRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final AccountStore accounts;

  public JwtAuthenticationFilter(JwtService jwtService, AccountStore accounts) {
    this.jwtService = jwtService;
    this.accounts = accounts;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null
        && header.startsWith("Bearer ")
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      jwtService
          .subject(header.substring(7))
          .flatMap(accounts::findByEmail)
          .filter(Account::enabled)
          .ifPresent(
              user ->
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              user.email(), null, authoritiesFor(user))));
    }
    chain.doFilter(request, response);
  }

  private static Set<GrantedAuthority> authoritiesFor(Account user) {
    Set<AccountRole> roles = new LinkedHashSet<>();
    roles.add(user.role());
    if (user.additionalRoles() != null) roles.addAll(user.additionalRoles());
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
