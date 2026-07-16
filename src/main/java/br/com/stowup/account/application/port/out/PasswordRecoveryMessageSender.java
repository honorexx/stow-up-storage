package br.com.stowup.account.application.port.out;

import br.com.stowup.account.domain.PasswordRecoveryChannel;

public interface PasswordRecoveryMessageSender {
  void send(PasswordRecoveryChannel channel, String destination, String code, int expiresInMinutes);
}
