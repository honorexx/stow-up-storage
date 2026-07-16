package br.com.stowup.account.application;

public class InvalidCredentialsException extends RuntimeException {
  public InvalidCredentialsException() {
    super("E-mail ou senha inválidos.");
  }
}
