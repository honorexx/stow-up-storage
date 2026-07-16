package br.com.stowup.contract.application.port.out;

import br.com.stowup.contract.domain.ContractData;
import java.nio.file.Path;

public interface ContractDocumentGenerator {
  Path generate(ContractData data);
}
