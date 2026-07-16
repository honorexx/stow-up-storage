package br.com.stowup.account.application;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException() {
    super("Conta não encontrada.");
  }
}
