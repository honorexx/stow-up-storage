package br.com.stowup.legal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal")
public class LegalController {
  @GetMapping("/terms/current")
  public LegalDocumentView currentTerms() {
    return new LegalDocumentView(
        TermsPolicy.CODE,
        TermsPolicy.CURRENT_VERSION,
        TermsPolicy.TITLE,
        TermsPolicy.SUMMARY,
        "/legal/termos-de-uso-" + TermsPolicy.CURRENT_VERSION + ".pdf");
  }

  public record LegalDocumentView(
      String code, String version, String title, String summary, String pdfUrl) {}
}
