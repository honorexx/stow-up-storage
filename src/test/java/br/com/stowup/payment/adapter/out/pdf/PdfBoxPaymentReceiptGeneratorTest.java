package br.com.stowup.payment.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.stowup.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxPaymentReceiptGeneratorTest {
  @Test
  void generatesValidPdfReceipt() throws Exception {
    PaymentRecord payment =
        new PaymentRecord(
            "TX-1",
            "RSV-1",
            "PREF-1",
            "PAY-1",
            "approved",
            new BigDecimal("86.40"),
            "Cliente",
            "cliente@stowup.test",
            "Box do Pedrão",
            Instant.parse("2026-07-16T18:00:00Z"),
            Instant.parse("2026-07-16T18:01:00Z"));

    byte[] pdf = new PdfBoxPaymentReceiptGenerator().generate(payment);

    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    assertThat(pdf.length).isGreaterThan(1000);
    try (var document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);
      assertThat(text)
          .contains("SITUAÇÃO DO PAGAMENTO")
          .contains("PREFERÊNCIA")
          .contains("ESPAÇO RESERVADO")
          .contains("Box do Pedrão")
          .contains("16/07/2026 às 15:01");
    }
  }
}
