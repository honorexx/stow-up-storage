package br.com.stowup.account.adapter.out.persistence;

import br.com.stowup.account.application.port.out.OwnedListingWithdrawal;
import br.com.stowup.space.SpaceRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaOwnedListingWithdrawal implements OwnedListingWithdrawal {
  private final SpaceRepository spaces;

  public JpaOwnedListingWithdrawal(SpaceRepository spaces) {
    this.spaces = spaces;
  }

  @Override
  public void withdrawAllByOwner(Long ownerId) {
    spaces.withdrawAllByOwnerId(ownerId);
  }
}
