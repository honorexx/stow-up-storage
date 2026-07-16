package br.com.stowup.favorite;

import br.com.stowup.space.*;
import br.com.stowup.user.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
  private final FavoriteRepository favorites;
  private final UserRepository users;
  private final SpaceRepository spaces;

  public FavoriteController(
      FavoriteRepository favorites, UserRepository users, SpaceRepository spaces) {
    this.favorites = favorites;
    this.users = users;
    this.spaces = spaces;
  }

  @GetMapping
  @Transactional(readOnly = true)
  public List<SpaceController.SpaceView> list(Principal p) {
    return favorites.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(p.getName()).stream()
        .map(f -> SpaceController.SpaceView.from(f.getSpace()))
        .toList();
  }

  @PostMapping("/{spaceId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public void add(@PathVariable Long spaceId, Principal p) {
    if (favorites.existsByUserEmailIgnoreCaseAndSpaceId(p.getName(), spaceId)) return;
    UserAccount u = users.findByEmailIgnoreCase(p.getName()).orElseThrow();
    Space s =
        spaces
            .findByIdAndAvailableTrueAndVerifiedTrue(spaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    favorites.save(new Favorite(u, s));
  }

  @DeleteMapping("/{spaceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void remove(@PathVariable Long spaceId, Principal p) {
    favorites.deleteByUserEmailIgnoreCaseAndSpaceId(p.getName(), spaceId);
  }

  @GetMapping("/{spaceId}")
  @Transactional(readOnly = true)
  public java.util.Map<String, Boolean> status(@PathVariable Long spaceId, Principal p) {
    return java.util.Map.of(
        "favorite", favorites.existsByUserEmailIgnoreCaseAndSpaceId(p.getName(), spaceId));
  }
}
