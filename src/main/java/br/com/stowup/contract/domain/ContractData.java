package br.com.stowup.contract.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractData(
    String bookingId,
    String contractCode,
    String ownerName,
    String ownerEmail,
    String customerName,
    String customerEmail,
    String spaceTitle,
    String spaceType,
    int spaceSize,
    String spaceAddress,
    LocalDate startDate,
    int months,
    BigDecimal total) {}
