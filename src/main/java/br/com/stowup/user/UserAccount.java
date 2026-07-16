package br.com.stowup.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 180)
  private String email;

  @Column(nullable = false, name = "password_hash")
  private String passwordHash;

  @Column(name = "staff_code", unique = true, length = 8)
  private String staffCode;

  @Column(length = 24)
  private String phone;

  @Column(name = "phone_normalized", length = 20)
  private String phoneNormalized;

  @Column(name = "cpf_encrypted", length = 512)
  private String cpfEncrypted;

  @Column(name = "cpf_hash", length = 64)
  private String cpfHash;

  @Column(name = "postal_code", length = 8)
  private String postalCode;

  @Column(name = "address_line", length = 180)
  private String addressLine;

  @Column(name = "address_number", length = 20)
  private String addressNumber;

  @Column(name = "address_complement", length = 80)
  private String addressComplement;

  @Column(length = 80)
  private String district;

  @Column(length = 80)
  private String city;

  @Column(length = 2)
  private String state;

  @Column(name = "photo_url", length = 300)
  private String photoUrl;

  @Column(name = "owner_verified", nullable = false)
  private boolean ownerVerified;

  private Double latitude;
  private Double longitude;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.CUSTOMER;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected UserAccount() {}

  public UserAccount(String name, String email, String passwordHash, Role role) {
    this.name = name;
    this.email = email.toLowerCase().trim();
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getStaffCode() {
    return staffCode;
  }

  public String getPhone() {
    return phone;
  }

  public String getPhoneNormalized() {
    return phoneNormalized;
  }

  public String getCpfEncrypted() {
    return cpfEncrypted;
  }

  public String getCpfHash() {
    return cpfHash;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getAddressNumber() {
    return addressNumber;
  }

  public String getAddressComplement() {
    return addressComplement;
  }

  public String getDistrict() {
    return district;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public boolean isOwnerVerified() {
    return ownerVerified;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public Role getRole() {
    return role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPhone(String phone) {
    this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    this.phoneNormalized = normalizePhone(phone);
  }

  public void setCpfEncrypted(String cpfEncrypted) {
    this.cpfEncrypted = cpfEncrypted;
  }

  public void setCpfHash(String cpfHash) {
    this.cpfHash = cpfHash;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public void setAddressLine(String addressLine) {
    this.addressLine = addressLine;
  }

  public void setAddressNumber(String addressNumber) {
    this.addressNumber = addressNumber;
  }

  public void setAddressComplement(String addressComplement) {
    this.addressComplement = addressComplement;
  }

  public void setDistrict(String district) {
    this.district = district;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
  }

  public void setOwnerVerified(boolean ownerVerified) {
    this.ownerVerified = ownerVerified;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setStaffCode(String staffCode) {
    this.staffCode = staffCode;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void close(String anonymizedEmail, String unusablePasswordHash) {
    this.name = "Conta excluída";
    this.email = anonymizedEmail;
    this.passwordHash = unusablePasswordHash;
    this.staffCode = null;
    this.phone = null;
    this.phoneNormalized = null;
    this.cpfEncrypted = null;
    this.cpfHash = null;
    this.postalCode = null;
    this.addressLine = null;
    this.addressNumber = null;
    this.addressComplement = null;
    this.district = null;
    this.city = null;
    this.state = null;
    this.photoUrl = null;
    this.ownerVerified = false;
    this.latitude = null;
    this.longitude = null;
    this.role = Role.CUSTOMER;
    this.enabled = false;
  }

  private static String normalizePhone(String value) {
    if (value == null || value.isBlank()) return null;
    String digits = value.replaceAll("\\D", "");
    return digits.length() == 10 || digits.length() == 11 ? "55" + digits : digits;
  }
}
