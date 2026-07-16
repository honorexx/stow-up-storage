package br.com.stowup.booking;

import br.com.stowup.space.Space;
import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
  @Id
  @Column(length = 20)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "space_id", nullable = false)
  private Space space;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private UserAccount customer;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(nullable = false)
  private int months;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status;

  @Column(nullable = false, unique = true, length = 20)
  private String contractCode;

  @Column(nullable = false, length = 30)
  private String paymentMethod;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "customer_checkout_at")
  private Instant customerCheckoutAt;

  @Column(name = "owner_checkout_at")
  private Instant ownerCheckoutAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "reserved_capacity", nullable = false)
  private int reservedCapacity;

  protected Booking() {}

  public Booking(
      String id,
      Space space,
      UserAccount customer,
      LocalDate startDate,
      int months,
      BigDecimal total,
      BookingStatus status,
      String contractCode,
      String paymentMethod) {
    this.id = id;
    this.space = space;
    this.customer = customer;
    this.startDate = startDate;
    this.endDate = startDate.plusMonths(months);
    this.months = months;
    this.total = total;
    this.status = status;
    this.contractCode = contractCode;
    this.paymentMethod = paymentMethod;
    this.reservedCapacity = space.getCapacityTotal();
  }

  public String getId() {
    return id;
  }

  public Space getSpace() {
    return space;
  }

  public UserAccount getCustomer() {
    return customer;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public int getMonths() {
    return months;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public String getContractCode() {
    return contractCode;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCustomerCheckoutAt() {
    return customerCheckoutAt;
  }

  public Instant getOwnerCheckoutAt() {
    return ownerCheckoutAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public int getReservedCapacity() {
    return reservedCapacity;
  }

  public void setStatus(BookingStatus status) {
    this.status = status;
  }

  public boolean confirmCheckout(boolean customer, LocalDate confirmationDate) {
    if (customer) customerCheckoutAt = Instant.now();
    else ownerCheckoutAt = Instant.now();
    boolean rentalPeriodEnded = !confirmationDate.isBefore(endDate);
    if ((rentalPeriodEnded && customerCheckoutAt != null)
        || (customerCheckoutAt != null && ownerCheckoutAt != null)) {
      status = BookingStatus.COMPLETED;
      completedAt = Instant.now();
      return true;
    }
    return false;
  }
}
