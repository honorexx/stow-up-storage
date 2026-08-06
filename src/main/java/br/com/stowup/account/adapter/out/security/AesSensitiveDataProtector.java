package br.com.stowup.account.adapter.out.security;

import br.com.stowup.account.application.port.out.SensitiveDataProtector;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesSensitiveDataProtector implements SensitiveDataProtector {
  private static final int IV_LENGTH = 12;
  private final SecretKeySpec encryptionKey;
  private final SecretKeySpec fingerprintKey;
  private final SecureRandom random = new SecureRandom();

  public AesSensitiveDataProtector(@Value("${stowup.data.encryption-key}") String secret) {
    if (secret == null || secret.length() < 32)
      throw new IllegalStateException("DATA_ENCRYPTION_KEY deve possuir pelo menos 32 caracteres.");
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
      encryptionKey = new SecretKeySpec(digest, "AES");
      fingerprintKey = new SecretKeySpec(digest, "HmacSHA256");
    } catch (Exception exception) {
      throw new IllegalStateException("Não foi possível configurar a proteção de dados", exception);
    }
  }

  @Override
  public String protect(String value) {
    if (value == null) return null;
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder()
          .encodeToString(
              ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
    } catch (Exception exception) {
      throw new IllegalStateException("Não foi possível proteger o dado sensível", exception);
    }
  }

  @Override
  public String reveal(String protectedValue) {
    if (protectedValue == null) return null;
    try {
      ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(protectedValue));
      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("Não foi possível ler o dado sensível", exception);
    }
  }

  @Override
  public String fingerprint(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(fingerprintKey);
      return java.util.HexFormat.of()
          .formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Não foi possível identificar o dado sensível", exception);
    }
  }
}
