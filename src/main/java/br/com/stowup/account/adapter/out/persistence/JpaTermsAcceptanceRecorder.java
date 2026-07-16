package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.application.port.out.TermsAcceptanceRecorder;
import org.springframework.stereotype.Component;

@Component
public class JpaTermsAcceptanceRecorder implements TermsAcceptanceRecorder {
  private final LegalAcceptanceRepository repository;

  public JpaTermsAcceptanceRecorder(LegalAcceptanceRepository repository) {
    this.repository = repository;
  }

  @Override
  public void record(Long accountId, String documentCode, String documentVersion) {
    if (!repository.existsByUserIdAndDocumentCodeAndDocumentVersion(
        accountId, documentCode, documentVersion))
      repository.save(new LegalAcceptanceEntity(accountId, documentCode, documentVersion));
  }
}
