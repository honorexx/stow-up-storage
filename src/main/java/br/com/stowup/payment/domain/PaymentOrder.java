package br.com.stowup.payment.domain;

import java.math.BigDecimal;

public record PaymentOrder(
    String bookingId, String title, BigDecimal amount, String customerName, String customerEmail) {}
