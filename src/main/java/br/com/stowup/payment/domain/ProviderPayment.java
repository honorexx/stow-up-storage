package br.com.stowup.payment.domain;

import java.math.BigDecimal;

public record ProviderPayment(
    String paymentId,
    String externalReference,
    String status,
    BigDecimal amount,
    String currency) {}
