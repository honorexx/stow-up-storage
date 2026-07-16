package br.com.stowup.listingdocument.application.port.out;

import br.com.stowup.space.Space;
import java.nio.file.Path;

public interface ListingDocumentGenerator {
  Path generate(Space space);
}
