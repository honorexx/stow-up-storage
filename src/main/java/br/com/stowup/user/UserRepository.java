package br.com.stowup.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
  List<UserAccount> findAllByOrderByCreatedAtDesc();

  Optional<UserAccount> findByEmailIgnoreCase(String email);

  Optional<UserAccount> findByStaffCodeIgnoreCase(String staffCode);

  Optional<UserAccount> findByPhoneNormalized(String phoneNormalized);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByStaffCodeIgnoreCase(String staffCode);

  boolean existsByPhoneNormalized(String phoneNormalized);

  boolean existsByCpfHash(String cpfHash);

  long countByRole(Role role);

  long countByOwnerVerifiedTrue();
}
