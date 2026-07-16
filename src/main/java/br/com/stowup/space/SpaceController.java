package br.com.stowup.space;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
  private final SpaceRepository spaces;

  public SpaceController(SpaceRepository spaces) {
    this.spaces = spaces;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<SpaceView> list(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(defaultValue = "Todos") String type,
      @RequestParam(defaultValue = "") String city,
      @RequestParam(defaultValue = "") String district,
      @RequestParam(defaultValue = "") String postalCode,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) Integer minSize,
      @RequestParam(required = false) Integer maxSize,
      @RequestParam(defaultValue = "") List<String> features,
      @RequestParam(required = false) Double userLatitude,
      @RequestParam(required = false) Double userLongitude,
      @RequestParam(defaultValue = "relevance") String sort) {
    String term = query.toLowerCase(Locale.ROOT).trim();
    Comparator<Space> comparator =
        switch (sort) {
          case "priceAsc", "price" -> Comparator.comparing(Space::getPrice);
          case "priceDesc" -> Comparator.comparing(Space::getPrice).reversed();
          case "rating" -> Comparator.comparingDouble(Space::getRating).reversed();
          case "size" -> Comparator.comparingInt(Space::getSize).reversed();
          case "recent" -> Comparator.comparing(Space::getId).reversed();
          case "distance" ->
              Comparator.comparingDouble(space -> distance(space, userLatitude, userLongitude));
          default ->
              Comparator.comparing(Space::isVerified)
                  .reversed()
                  .thenComparingDouble(space -> distance(space, userLatitude, userLongitude));
        };

    return spaces.findByAvailableTrueAndVerifiedTrue().stream()
        .filter(space -> type.equalsIgnoreCase("Todos") || space.getType().equalsIgnoreCase(type))
        .filter(
            space ->
                term.isBlank()
                    || searchable(space).contains(term)
                    || term.contains(space.getCity().toLowerCase(Locale.ROOT)))
        .filter(space -> city.isBlank() || space.getCity().equalsIgnoreCase(city.trim()))
        .filter(
            space -> district.isBlank() || space.getDistrict().equalsIgnoreCase(district.trim()))
        .filter(
            space ->
                postalCode.isBlank()
                    || java.util.Objects.equals(
                        space.getPostalCode(), postalCode.replaceAll("\\D", "")))
        .filter(space -> minPrice == null || space.getPrice().compareTo(minPrice) >= 0)
        .filter(space -> maxPrice == null || space.getPrice().compareTo(maxPrice) <= 0)
        .filter(space -> minSize == null || space.getSize() >= minSize)
        .filter(space -> maxSize == null || space.getSize() <= maxSize)
        .filter(
            space ->
                features.isEmpty()
                    || space.getFeatures().stream()
                        .map(v -> v.toLowerCase(Locale.ROOT))
                        .toList()
                        .containsAll(
                            features.stream()
                                .filter(v -> !v.isBlank())
                                .map(v -> v.toLowerCase(Locale.ROOT))
                                .toList()))
        .sorted(comparator)
        .map(space -> SpaceView.from(space, userLatitude, userLongitude))
        .toList();
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public SpaceView detail(@PathVariable Long id) {
    return spaces
        .findByIdAndAvailableTrueAndVerifiedTrue(id)
        .map(SpaceView::from)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espaço não encontrado."));
  }

  private String searchable(Space space) {
    return (space.getTitle()
            + " "
            + space.getCity()
            + " "
            + space.getDistrict()
            + " "
            + space.getPostalCode()
            + " "
            + space.getType()
            + " "
            + String.join(" ", space.getFeatures()))
        .toLowerCase(Locale.ROOT);
  }

  private static double distance(Space space, Double lat, Double lon) {
    if (lat == null || lon == null || space.getLatitude() == null || space.getLongitude() == null)
      return space.getDistance();
    double p1 = Math.toRadians(lat),
        p2 = Math.toRadians(space.getLatitude()),
        dp = Math.toRadians(space.getLatitude() - lat),
        dl = Math.toRadians(space.getLongitude() - lon);
    double a =
        Math.sin(dp / 2) * Math.sin(dp / 2)
            + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  public record SpaceView(
      Long id,
      String title,
      String city,
      String district,
      String type,
      int size,
      java.math.BigDecimal heightMeters,
      java.math.BigDecimal lengthMeters,
      java.math.BigDecimal depthMeters,
      java.math.BigDecimal volumeCubicMeters,
      java.math.BigDecimal price,
      double rating,
      int reviews,
      double distance,
      Double latitude,
      Double longitude,
      boolean verified,
      boolean available,
      List<String> features,
      List<String> photos,
      String description,
      String owner,
      Long ownerId,
      boolean ownerVerified,
      String lifecycleStatus,
      int capacityTotal,
      int capacityUsed,
      int capacityAvailable,
      String ownerSince,
      String toneA,
      String toneB) {
    public static SpaceView from(Space space) {
      return from(space, null, null);
    }

    public static SpaceView from(Space space, Double userLatitude, Double userLongitude) {
      return new SpaceView(
          space.getId(),
          space.getTitle(),
          space.getCity(),
          space.getDistrict(),
          space.getType(),
          space.getSize(),
          space.getHeightMeters(),
          space.getLengthMeters(),
          space.getDepthMeters(),
          space.getVolumeCubicMeters(),
          space.getPrice(),
          space.getRating(),
          space.getReviews(),
          SpaceController.distance(space, userLatitude, userLongitude),
          space.getLatitude(),
          space.getLongitude(),
          space.isVerified(),
          space.isAvailable(),
          space.getFeatures(),
          space.getPhotoUrls(),
          space.getDescription(),
          space.getOwner().getName(),
          space.getOwner().getId(),
          space.getOwner().isOwnerVerified(),
          space.getLifecycleStatus(),
          space.getCapacityTotal(),
          space.getCapacityUsed(),
          space.getCapacityAvailable(),
          space.getOwner().isOwnerVerified()
              ? "Proprietário verificado"
              : "Identidade ainda não verificada",
          space.getToneA(),
          space.getToneB());
    }
  }
}
