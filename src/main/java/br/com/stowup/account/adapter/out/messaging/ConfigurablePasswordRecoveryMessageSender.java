package br.com.stowup.account.adapter.out.messaging;

import br.com.stowup.account.application.PasswordRecoveryDeliveryException;
import br.com.stowup.account.application.port.out.PasswordRecoveryMessageSender;
import br.com.stowup.account.domain.PasswordRecoveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class ConfigurablePasswordRecoveryMessageSender implements PasswordRecoveryMessageSender {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConfigurablePasswordRecoveryMessageSender.class);
  private final ObjectProvider<JavaMailSender> mailSender;
  private final RestClient restClient;
  private final String deliveryMode;
  private final String emailFrom;
  private final String twilioAccountSid;
  private final String twilioAuthToken;
  private final String twilioFromNumber;

  public ConfigurablePasswordRecoveryMessageSender(
      ObjectProvider<JavaMailSender> mailSender,
      RestClient.Builder restClient,
      @Value("${stowup.recovery.delivery-mode:log}") String deliveryMode,
      @Value("${stowup.recovery.email-from:no-reply@stowup.local}") String emailFrom,
      @Value("${stowup.recovery.twilio.account-sid:}") String twilioAccountSid,
      @Value("${stowup.recovery.twilio.auth-token:}") String twilioAuthToken,
      @Value("${stowup.recovery.twilio.from-number:}") String twilioFromNumber) {
    this.mailSender = mailSender;
    this.restClient = restClient.build();
    this.deliveryMode = deliveryMode;
    this.emailFrom = emailFrom;
    this.twilioAccountSid = twilioAccountSid;
    this.twilioAuthToken = twilioAuthToken;
    this.twilioFromNumber = twilioFromNumber;
  }

  @Override
  public void send(
      PasswordRecoveryChannel channel, String destination, String code, int expiresInMinutes) {
    if ("log".equalsIgnoreCase(deliveryMode)) {
      LOGGER.warn(
          "MODO LOCAL - código de recuperação StowUp: {} (canal {}, validade {} min)",
          code,
          channel,
          expiresInMinutes);
      return;
    }
    try {
      if (channel == PasswordRecoveryChannel.EMAIL) sendEmail(destination, code, expiresInMinutes);
      else sendSms(destination, code, expiresInMinutes);
    } catch (IllegalStateException exception) {
      throw new PasswordRecoveryDeliveryException(exception.getMessage(), exception);
    } catch (Exception exception) {
      throw new PasswordRecoveryDeliveryException(
          "Não foi possível enviar o código agora. Tente novamente em instantes.", exception);
    }
  }

  private void sendEmail(String destination, String code, int expiresInMinutes) {
    JavaMailSender sender =
        mailSender.getIfAvailable(
            () -> {
              throw new IllegalStateException("Servidor SMTP não configurado.");
            });
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(emailFrom);
    message.setTo(destination);
    message.setSubject("Seu código de recuperação StowUp");
    message.setText(
        "Seu código StowUp é "
            + code
            + ". Ele expira em "
            + expiresInMinutes
            + " minutos. Se você não fez esta solicitação, ignore esta mensagem.");
    sender.send(message);
  }

  private void sendSms(String destination, String code, int expiresInMinutes) {
    if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank() || twilioFromNumber.isBlank())
      throw new IllegalStateException("Twilio não configurado.");
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("To", destination);
    form.add("From", twilioFromNumber);
    form.add(
        "Body", "StowUp: seu código é " + code + ". Expira em " + expiresInMinutes + " minutos.");
    restClient
        .post()
        .uri(
            "https://api.twilio.com/2010-04-01/Accounts/{accountSid}/Messages.json",
            twilioAccountSid)
        .headers(headers -> headers.setBasicAuth(twilioAccountSid, twilioAuthToken))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .toBodilessEntity();
  }
}
