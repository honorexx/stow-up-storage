package br.com.stowup.common;

import br.com.stowup.account.application.AccountConflictException;
import br.com.stowup.account.application.AccountNotFoundException;
import br.com.stowup.account.application.InvalidAccountDataException;
import br.com.stowup.account.application.InvalidCredentialsException;
import br.com.stowup.account.application.PasswordRecoveryDeliveryException;
import br.com.stowup.account.application.PasswordRecoveryException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
    Map<String, String> fields = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(body(HttpStatus.BAD_REQUEST, "Revise os dados informados.", fields));
  }

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<Map<String, Object>> credentials(BadCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(body(HttpStatus.UNAUTHORIZED, exception.getMessage(), null));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<Map<String, Object>> invalidCredentials(InvalidCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(body(HttpStatus.UNAUTHORIZED, exception.getMessage(), null));
  }

  @ExceptionHandler(AccountConflictException.class)
  ResponseEntity<Map<String, Object>> conflict(AccountConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(body(HttpStatus.CONFLICT, exception.getMessage(), null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String, Object>> uniqueConstraint(DataIntegrityViolationException exception) {
    String details = exception.getMostSpecificCause().getMessage().toLowerCase();
    String message =
        details.contains("email")
            ? "Este e-mail já está cadastrado."
            : details.contains("phone")
                ? "Este celular já está cadastrado."
                : "Já existe um cadastro com estes dados.";
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(body(HttpStatus.CONFLICT, message, null));
  }

  @ExceptionHandler(InvalidAccountDataException.class)
  ResponseEntity<Map<String, Object>> invalidAccount(InvalidAccountDataException exception) {
    return ResponseEntity.badRequest()
        .body(body(HttpStatus.BAD_REQUEST, exception.getMessage(), null));
  }

  @ExceptionHandler(AccountNotFoundException.class)
  ResponseEntity<Map<String, Object>> accountNotFound(AccountNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(body(HttpStatus.NOT_FOUND, exception.getMessage(), null));
  }

  @ExceptionHandler(PasswordRecoveryException.class)
  ResponseEntity<Map<String, Object>> passwordRecovery(PasswordRecoveryException exception) {
    return ResponseEntity.badRequest()
        .body(body(HttpStatus.BAD_REQUEST, exception.getMessage(), null));
  }

  @ExceptionHandler(PasswordRecoveryDeliveryException.class)
  ResponseEntity<Map<String, Object>> passwordDelivery(
      PasswordRecoveryDeliveryException exception) {
    LOGGER.error("Falha ao entregar código de recuperação", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(body(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), null));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, Object>> status(ResponseStatusException exception) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    return ResponseEntity.status(status).body(body(status, exception.getReason(), null));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Map<String, Object>> resourceNotFound(NoResourceFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(body(HttpStatus.NOT_FOUND, "Recurso não encontrado.", null));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String, Object>> uploadTooLarge(MaxUploadSizeExceededException exception) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            body(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "A foto excede o limite de 10 MB. Escolha uma imagem menor.",
                null));
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  ResponseEntity<Map<String, Object>> businessRule(RuntimeException exception) {
    return ResponseEntity.badRequest()
        .body(body(HttpStatus.BAD_REQUEST, exception.getMessage(), null));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
    LOGGER.error("Falha inesperada ao processar requisição", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            body(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação.", null));
  }

  private Map<String, Object> body(HttpStatus status, String message, Object details) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("timestamp", Instant.now());
    result.put("status", status.value());
    result.put("error", status.getReasonPhrase());
    result.put("message", message == null ? "Não foi possível concluir a operação." : message);
    if (details != null) result.put("details", details);
    return result;
  }
}
