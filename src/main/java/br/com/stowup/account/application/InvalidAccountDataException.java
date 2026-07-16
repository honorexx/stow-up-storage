package br.com.stowup.account.application;

public class InvalidAccountDataException extends RuntimeException {
  public InvalidAccountDataException(String message) {
    super(message);
  }
}
