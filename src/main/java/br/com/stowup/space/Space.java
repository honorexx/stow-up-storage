package br.com.stowup.space;

import br.com.stowup.user.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "spaces")
public class Space {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 140)
  private String title;

  @Column(nullable = false, length = 80)
  private String city;

  @Column(nullable = false, length = 80)
  private String district;

  @Column(name = "postal_code", length = 8)
  private String postalCode;

  @Column(name = "address_line", length = 180)
  private String addressLine;

  @Column(name = "address_number", length = 20)
  private String addressNumber;

  @Column(name = "address_complement", length = 80)
  private String addressComplement;

  @Column(length = 2)
  private String state;

  @Column(nullable = false, length = 30)
  private String type;

  @Column(nullable = false)
  private int size;

  @Column(name = "height_meters", nullable = false, precision = 8, scale = 2)
  private BigDecimal heightMeters = BigDecimal.valueOf(2);

  @Column(name = "length_meters", nullable = false, precision = 8, scale = 2)
  private BigDecimal lengthMeters = BigDecimal.ONE;

  @Column(name = "depth_meters", nullable = false, precision = 8, scale = 2)
  private BigDecimal depthMeters = BigDecimal.ONE;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private double rating;

  @Column(nullable = false)
  private int reviews;

  @Column(nullable = false)
  private double distance;

  private Double latitude;
  private Double longitude;

  @Column(nullable = false)
  private boolean verified;

  @Column(nullable = false)
  private boolean available = true;

  @Column(nullable = false, length = 20)
  private String moderationStatus = "PENDING";

  @Column(length = 500)
  private String moderationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moderated_by")
  private UserAccount moderatedBy;

  private Instant moderatedAt;

  @Column(name = "lifecycle_status", nullable = false, length = 30)
  private String lifecycleStatus = "IN_REVIEW";

  @Column(name = "capacity_total", nullable = false)
  private int capacityTotal;

  @Column(name = "capacity_used", nullable = false)
  private int capacityUsed;

  @Column(nullable = false, length = 1200)
  private String description;

  @Column(nullable = false, length = 600)
  private String featuresCsv;

  @Column(name = "tone_a", nullable = false, length = 12)
  private String toneA;

  @Column(name = "tone_b", nullable = false, length = 12)
  private String toneB;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private UserAccount owner;

  @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder asc")
  private List<SpacePhoto> photos = new ArrayList<>();

  protected Space() {}

  public Space(
      String title,
      String city,
      String district,
      String type,
      int size,
      BigDecimal price,
      double rating,
      int reviews,
      double distance,
      boolean verified,
      String description,
      List<String> features,
      String toneA,
      String toneB,
      UserAccount owner) {
    this(
        title,
        city,
        district,
        type,
        size,
        price,
        rating,
        reviews,
        distance,
        null,
        null,
        verified,
        description,
        features,
        toneA,
        toneB,
        owner);
  }

  public Space(
      String title,
      String city,
      String district,
      String type,
      int size,
      BigDecimal price,
      double rating,
      int reviews,
      double distance,
      Double latitude,
      Double longitude,
      boolean verified,
      String description,
      List<String> features,
      String toneA,
      String toneB,
      UserAccount owner) {
    this(
        title,
        city,
        district,
        null,
        null,
        null,
        null,
        null,
        type,
        size,
        price,
        rating,
        reviews,
        distance,
        latitude,
        longitude,
        verified,
        description,
        features,
        toneA,
        toneB,
        owner);
  }

  public Space(
      String title,
      String city,
      String district,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String state,
      String type,
      int size,
      BigDecimal price,
      double rating,
      int reviews,
      double distance,
      Double latitude,
      Double longitude,
      boolean verified,
      String description,
      List<String> features,
      String toneA,
      String toneB,
      UserAccount owner) {
    this.title = title;
    this.city = city;
    this.district = district;
    this.postalCode = postalCode;
    this.addressLine = addressLine;
    this.addressNumber = addressNumber;
    this.addressComplement = addressComplement;
    this.state = state;
    this.type = type;
    this.size = size;
    this.lengthMeters = BigDecimal.valueOf(Math.max(1, Math.sqrt(size)));
    this.depthMeters = BigDecimal.valueOf(Math.max(1, Math.sqrt(size)));
    this.capacityTotal = size;
    this.price = price;
    this.rating = rating;
    this.reviews = reviews;
    this.distance = distance;
    this.latitude = latitude;
    this.longitude = longitude;
    this.verified = verified;
    this.moderationStatus = verified ? "APPROVED" : "PENDING";
    this.lifecycleStatus = verified ? "ACTIVE" : "IN_REVIEW";
    this.description = description;
    this.featuresCsv = String.join("|", features);
    this.toneA = toneA;
    this.toneB = toneB;
    this.owner = owner;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getCity() {
    return city;
  }

  public String getDistrict() {
    return district;
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

  public String getState() {
    return state;
  }

  public String getType() {
    return type;
  }

  public int getSize() {
    return size;
  }

  public BigDecimal getHeightMeters() {
    return heightMeters;
  }

  public BigDecimal getLengthMeters() {
    return lengthMeters;
  }

  public BigDecimal getDepthMeters() {
    return depthMeters;
  }

  public BigDecimal getVolumeCubicMeters() {
    return heightMeters
        .multiply(lengthMeters)
        .multiply(depthMeters)
        .setScale(2, java.math.RoundingMode.HALF_UP);
  }

  public BigDecimal getPrice() {
    return price;
  }

  public double getRating() {
    return rating;
  }

  public int getReviews() {
    return reviews;
  }

  public double getDistance() {
    return distance;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public boolean isVerified() {
    return verified;
  }

  public boolean isAvailable() {
    return available;
  }

  public String getModerationStatus() {
    return moderationStatus;
  }

  public String getModerationReason() {
    return moderationReason;
  }

  public Instant getModeratedAt() {
    return moderatedAt;
  }

  public String getLifecycleStatus() {
    return lifecycleStatus;
  }

  public int getCapacityTotal() {
    return capacityTotal;
  }

  public int getCapacityUsed() {
    return capacityUsed;
  }

  public int getCapacityAvailable() {
    return Math.max(0, capacityTotal - capacityUsed);
  }

  public String getDescription() {
    return description;
  }

  public List<String> getFeatures() {
    return featuresCsv == null || featuresCsv.isBlank()
        ? List.of()
        : Arrays.asList(featuresCsv.split("\\|"));
  }

  public String getToneA() {
    return toneA;
  }

  public String getToneB() {
    return toneB;
  }

  public UserAccount getOwner() {
    return owner;
  }

  public List<String> getPhotoUrls() {
    return photos.stream().map(SpacePhoto::getUrl).toList();
  }

  public void addPhoto(String url) {
    photos.add(new SpacePhoto(this, url, photos.size()));
  }

  public void setAvailable(boolean available) {
    this.available = available;
    this.lifecycleStatus = available ? (verified ? "ACTIVE" : "IN_REVIEW") : "TEMP_UNAVAILABLE";
  }

  public void setVerified(boolean verified) {
    this.verified = verified;
  }

  public void setCapacityTotal(int capacityTotal) {
    if (capacityTotal < 1 || capacityTotal < capacityUsed)
      throw new IllegalArgumentException("Capacidade total inválida.");
    this.capacityTotal = capacityTotal;
  }

  public void setDimensions(BigDecimal height, BigDecimal length, BigDecimal depth) {
    if (height == null
        || length == null
        || depth == null
        || height.signum() <= 0
        || length.signum() <= 0
        || depth.signum() <= 0)
      throw new IllegalArgumentException("Informe altura, comprimento e profundidade válidos.");
    heightMeters = height.setScale(2, java.math.RoundingMode.HALF_UP);
    lengthMeters = length.setScale(2, java.math.RoundingMode.HALF_UP);
    depthMeters = depth.setScale(2, java.math.RoundingMode.HALF_UP);
    size =
        Math.max(
            1,
            lengthMeters
                .multiply(depthMeters)
                .setScale(0, java.math.RoundingMode.CEILING)
                .intValue());
    capacityTotal =
        Math.max(
            capacityUsed,
            Math.max(
                1, getVolumeCubicMeters().setScale(0, java.math.RoundingMode.CEILING).intValue()));
  }

  public void setRatingSummary(double rating, int reviews) {
    this.rating = rating;
    this.reviews = reviews;
  }

  public void occupy(int amount) {
    capacityUsed = Math.min(capacityTotal, capacityUsed + Math.max(0, amount));
    lifecycleStatus = capacityUsed >= capacityTotal ? "OCCUPIED" : "ACTIVE";
    available = capacityUsed < capacityTotal;
  }

  public void release(int amount) {
    capacityUsed = Math.max(0, capacityUsed - Math.max(0, amount));
    if (verified) {
      lifecycleStatus = "ACTIVE";
      available = true;
    }
  }

  public void suspendTemporarily() {
    available = false;
    lifecycleStatus = "TEMP_UNAVAILABLE";
  }

  public void reactivate() {
    if (!verified) throw new IllegalStateException("O anúncio precisa estar aprovado.");
    available = capacityUsed < capacityTotal;
    lifecycleStatus = available ? "ACTIVE" : "OCCUPIED";
  }

  public void suspendByAdmin(String reason) {
    available = false;
    lifecycleStatus = "SUSPENDED";
    moderationReason = reason;
  }

  public void updateByAdmin(
      String title,
      String description,
      String type,
      BigDecimal height,
      BigDecimal length,
      BigDecimal depth,
      BigDecimal price,
      String city,
      String district,
      String postalCode,
      String addressLine,
      String addressNumber,
      String addressComplement,
      String state) {
    this.title = title.trim();
    this.description = description.trim();
    this.type = type.trim();
    setDimensions(height, length, depth);
    this.price = price;
    this.city = city.trim();
    this.district = district.trim();
    this.postalCode = postalCode == null ? null : postalCode.replaceAll("\\D", "");
    this.addressLine = addressLine.trim();
    this.addressNumber = addressNumber.trim();
    this.addressComplement =
        addressComplement == null || addressComplement.isBlank() ? null : addressComplement.trim();
    this.state = state.trim().toUpperCase();
  }

  public void withdrawByAdmin(String reason) {
    available = false;
    verified = false;
    moderationStatus = "WITHDRAWN";
    lifecycleStatus = "WITHDRAWN";
    moderationReason = reason;
  }

  public void withdrawByOwner() {
    available = false;
    verified = false;
    moderationStatus = "WITHDRAWN";
    lifecycleStatus = "WITHDRAWN";
    moderationReason = "Excluído pelo proprietário";
  }

  public void approve(UserAccount moderator) {
    this.verified = true;
    this.available = true;
    this.moderationStatus = "APPROVED";
    this.lifecycleStatus = "ACTIVE";
    this.moderationReason = null;
    this.moderatedBy = moderator;
    this.moderatedAt = Instant.now();
  }

  public void reject(UserAccount moderator, String reason) {
    this.verified = false;
    this.available = false;
    this.moderationStatus = "REJECTED";
    this.lifecycleStatus = "REJECTED";
    this.moderationReason = reason.trim();
    this.moderatedBy = moderator;
    this.moderatedAt = Instant.now();
  }
}
