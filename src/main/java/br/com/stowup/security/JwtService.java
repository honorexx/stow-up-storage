package br.com.stowup.security;

import br.com.stowup.account.application.port.out.AccessTokenIssuer;
import br.com.stowup.account.domain.Account;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService implements AccessTokenIssuer {
  private final Algorithm algorithm;
  private final String issuer;
  private final long expirationHours;

  public JwtService(
      @Value("${stowup.jwt.secret}") String secret,
      @Value("${stowup.jwt.issuer}") String issuer,
      @Value("${stowup.jwt.expiration-hours}") long expirationHours) {
    if (secret == null || secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET deve possuir pelo menos 32 caracteres.");
    if (expirationHours < 1 || expirationHours > 168)
      throw new IllegalStateException("A validade do token deve ficar entre 1 e 168 horas.");
    this.algorithm = Algorithm.HMAC256(secret);
    this.issuer = issuer;
    this.expirationHours = expirationHours;
  }

  @Override
  public String issue(Account user) {
    Instant now = Instant.now();
    return JWT.create()
        .withIssuer(issuer)
        .withSubject(user.email())
        .withClaim("role", user.role().name())
        .withIssuedAt(now)
        .withExpiresAt(now.plus(expirationHours, ChronoUnit.HOURS))
        .sign(algorithm);
  }

  public Optional<String> subject(String token) {
    try {
      return Optional.of(
          JWT.require(algorithm).withIssuer(issuer).build().verify(token).getSubject());
    } catch (JWTVerificationException exception) {
      return Optional.empty();
    }
  }
}
