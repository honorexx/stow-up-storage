package br.com.stowup.listingdocument.application;

import br.com.stowup.space.SpaceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListingDocumentBootstrap implements ApplicationRunner {
  private final SpaceRepository spaces;
  private final ListingDocumentService documents;

  public ListingDocumentBootstrap(SpaceRepository spaces, ListingDocumentService documents) {
    this.spaces = spaces;
    this.documents = documents;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    spaces.findAll().forEach(documents::createFor);
  }
}
