package br.com.stowup.payment.adapter.in.web;

import br.com.stowup.payment.application.PaymentService;
import br.com.stowup.payment.domain.CheckoutSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService payments;

  public PaymentController(PaymentService payments) {
    this.payments = payments;
  }

  @PostMapping("/checkout")
  public CheckoutSession checkout(
      @Valid @RequestBody CheckoutRequest request, Principal principal) {
    return payments.createCheckout(request.bookingId(), principal.getName());
  }

  @PostMapping("/webhooks/mercado-pago")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void webhook(
      @RequestParam(name = "data.id", required = false) String dataId,
      @RequestBody(required = false) WebhookBody body) {
    String id =
        dataId != null ? dataId : body != null && body.data() != null ? body.data().id() : null;
    if (id != null && !id.isBlank()) payments.receiveProviderNotification(id);
  }

  public record CheckoutRequest(@NotBlank String bookingId) {}

  public record WebhookBody(String type, WebhookData data) {}

  public record WebhookData(String id) {}
}
