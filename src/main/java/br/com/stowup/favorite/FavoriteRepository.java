package br.com.stowup.favorite;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
  @EntityGraph(attributePaths = {"space", "space.owner", "space.photos"})
  List<Favorite> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

  boolean existsByUserEmailIgnoreCaseAndSpaceId(String email, Long spaceId);

  long deleteByUserEmailIgnoreCaseAndSpaceId(String email, Long spaceId);
}
