package br.com.stowup.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
  private final ReviewService service;

  public ReviewController(ReviewService service) {
    this.service = service;
  }

  @GetMapping("/eligible")
  public List<ReviewService.EligibleReview> eligible(Principal p) {
    return service.eligible(p.getName());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewService.ReviewView create(@Valid @RequestBody Request r, Principal p) {
    return service.create(r.bookingId(), p.getName(), r.rating(), r.comment());
  }

  public record Request(
      @NotBlank String bookingId, @Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}
}
