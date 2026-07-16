package br.com.stowup.account.application.port.out;

import br.com.stowup.account.domain.Account;

public interface AccessTokenIssuer {
  String issue(Account account);
}
