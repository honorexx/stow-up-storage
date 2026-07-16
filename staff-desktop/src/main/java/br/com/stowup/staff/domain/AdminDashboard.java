package br.com.stowup.staff.domain;

import java.math.BigDecimal;

public record AdminDashboard(
    long totalUsers,
    long customers,
    long owners,
    long verifiedOwners,
    long activeSpaces,
    long occupiedSpaces,
    long pendingSpaces,
    long ongoingBookings,
    long activeContracts,
    long pendingReports,
    BigDecimal platformRevenue) {}
