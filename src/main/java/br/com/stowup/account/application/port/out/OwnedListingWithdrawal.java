package br.com.stowup.account.application.port.out;

public interface OwnedListingWithdrawal {
  void withdrawAllByOwner(Long ownerId);
}
