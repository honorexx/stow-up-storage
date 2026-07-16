package br.com.stowup.account.application.port.out;

public interface TermsAcceptanceRecorder {
  void record(Long accountId, String documentCode, String documentVersion);
}
