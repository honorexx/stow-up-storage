package br.com.stowup.account.application.port.out;

public interface SensitiveDataProtector {
  String protect(String value);

  String reveal(String protectedValue);

  String fingerprint(String value);
}
