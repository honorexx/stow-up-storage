package br.com.stowup.notification;

import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class DocumentEmailService {
  private static final Logger log = LoggerFactory.getLogger(DocumentEmailService.class);
  private final ObjectProvider<JavaMailSender> mail;
  private final String deliveryMode;
  private final String from;

  public DocumentEmailService(
      ObjectProvider<JavaMailSender> mail,
      @Value("${stowup.recovery.delivery-mode:log}") String deliveryMode,
      @Value("${stowup.recovery.email-from:no-reply@stowup.local}") String from) {
    this.mail = mail;
    this.deliveryMode = deliveryMode;
    this.from = from;
  }

  public void sendPdf(String to, String subject, String message, Path attachment, String fileName) {
    if (!"live".equalsIgnoreCase(deliveryMode)) {
      log.info("E-mail de documento em modo local: destino={}, assunto={}", to, subject);
      return;
    }
    try {
      if (!Files.isRegularFile(attachment)) {
        log.warn("E-mail não enviado: anexo inexistente em {}", attachment);
        return;
      }
      JavaMailSender sender = mail.getIfAvailable();
      if (sender == null) {
        log.warn("E-mail de documento não enviado: servidor SMTP indisponível.");
        return;
      }
      MimeMessage mime = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(message, false);
      helper.addAttachment(fileName, attachment.toFile());
      sender.send(mime);
    } catch (Exception exception) {
      log.error("Não foi possível enviar o documento por e-mail para {}", to, exception);
    }
  }
}
