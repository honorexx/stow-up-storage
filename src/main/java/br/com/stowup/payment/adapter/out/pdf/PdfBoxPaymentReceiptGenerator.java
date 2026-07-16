package br.com.stowup.payment.adapter.out.pdf;

import br.com.stowup.payment.application.port.out.PaymentReceiptGenerator;
import br.com.stowup.payment.domain.PaymentRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
class PdfBoxPaymentReceiptGenerator implements PaymentReceiptGenerator {
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
          .withZone(ZoneId.of("America/Sao_Paulo"));
  private static final Locale BRAZIL = new Locale("pt", "BR");

  @Override
  public byte[] generate(PaymentRecord payment) {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        header(content, regular, bold, payment);
        statusCard(content, regular, bold, payment);
        detailsCard(content, regular, bold, payment);
        footer(content, regular, bold, payment);
      }

      document.save(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Não foi possível gerar o comprovante.", exception);
    }
  }

  private static void header(
      PDPageContentStream content, PDFont regular, PDFont bold, PaymentRecord payment)
      throws IOException {
    fill(content, 0, 742, 595, 100, 0, 43, 92);
    fill(content, 0, 738, 595, 4, 255, 90, 31);
    text(content, bold, 22, 48, 798, "StowUp", 255, 255, 255);
    text(content, bold, 14, 48, 773, "COMPROVANTE DE PAGAMENTO", 255, 255, 255);
    text(
        content,
        regular,
        8,
        430,
        797,
        "Reserva " + safe(payment.bookingId()),
        220,
        230,
        240);
    text(
        content,
        regular,
        8,
        430,
        779,
        DATE.format(payment.updatedAt()),
        220,
        230,
        240);
  }

  private static void statusCard(
      PDPageContentStream content, PDFont regular, PDFont bold, PaymentRecord payment)
      throws IOException {
    boolean approved = "approved".equalsIgnoreCase(payment.status());
    int[] background = approved ? new int[] {232, 247, 239} : new int[] {255, 242, 232};
    int[] foreground = approved ? new int[] {26, 127, 75} : new int[] {190, 83, 20};
    fill(
        content,
        48,
        656,
        499,
        56,
        background[0],
        background[1],
        background[2]);
    fill(
        content,
        48,
        656,
        6,
        56,
        foreground[0],
        foreground[1],
        foreground[2]);
    text(content, bold, 9, 70, 689, "SITUAÇÃO DO PAGAMENTO", 80, 96, 116);
    text(
        content,
        bold,
        14,
        70,
        670,
        statusLabel(payment.status()).toUpperCase(BRAZIL),
        foreground[0],
        foreground[1],
        foreground[2]);
    text(
        content,
        regular,
        8,
        392,
        681,
        "ID Mercado Pago",
        80,
        96,
        116);
    text(
        content,
        bold,
        9,
        392,
        668,
        safe(payment.providerPaymentId()),
        23,
        43,
        67);
  }

  private static void detailsCard(
      PDPageContentStream content, PDFont regular, PDFont bold, PaymentRecord payment)
      throws IOException {
    stroke(content, 48, 346, 499, 282, 220, 226, 233);
    text(content, bold, 11, 68, 601, "DADOS DO PAGAMENTO", 0, 43, 92);
    line(content, 68, 586, 527, 586, 230, 234, 239);

    float y = 559;
    y = row(content, regular, bold, y, "Cliente", payment.customerName());
    y = row(content, regular, bold, y, "E-mail", payment.customerEmail());
    y = row(content, regular, bold, y, "Espaço reservado", payment.spaceTitle());
    y =
        row(
            content,
            regular,
            bold,
            y,
            "Valor pago",
            NumberFormat.getCurrencyInstance(BRAZIL).format(payment.amount()));
    y = row(content, regular, bold, y, "Código da reserva", payment.bookingId());
    y =
        wrappedRow(
            content,
            regular,
            bold,
            y,
            "Preferência",
            payment.providerPreferenceId(),
            8,
            315);
    row(content, regular, bold, y, "Atualizado em", DATE.format(payment.updatedAt()));
  }

  private static void footer(
      PDPageContentStream content, PDFont regular, PDFont bold, PaymentRecord payment)
      throws IOException {
    text(content, bold, 9, 48, 305, "INFORMAÇÕES IMPORTANTES", 0, 43, 92);
    line(content, 48, 294, 547, 294, 230, 234, 239);
    paragraph(
        content,
        regular,
        8.5f,
        48,
        273,
        499,
        "Este comprovante foi emitido pela StowUp com base na confirmação recebida do Mercado Pago. "
            + "A StowUp não armazena o número completo do cartão nem o código de segurança.",
        80,
        96,
        116);
    text(
        content,
        regular,
        8,
        48,
        215,
        "Identificador interno: " + safe(payment.id()),
        115,
        128,
        145);
    line(content, 48, 90, 547, 90, 230, 234, 239);
    text(
        content,
        bold,
        8,
        48,
        70,
        "StowUp - Seu espaço, quando precisar.",
        0,
        43,
        92);
    text(
        content,
        regular,
        7.5f,
        395,
        70,
        "Documento gerado eletronicamente",
        115,
        128,
        145);
  }

  private static float row(
      PDPageContentStream content,
      PDFont regular,
      PDFont bold,
      float y,
      String label,
      String value)
      throws IOException {
    text(content, bold, 8.5f, 68, y, label.toUpperCase(BRAZIL), 96, 112, 134);
    text(content, regular, 10, 210, y, safe(value), 23, 43, 67);
    line(content, 68, y - 15, 527, y - 15, 238, 241, 244);
    return y - 34;
  }

  private static float wrappedRow(
      PDPageContentStream content,
      PDFont regular,
      PDFont bold,
      float y,
      String label,
      String value,
      float size,
      float maxWidth)
      throws IOException {
    text(content, bold, 8.5f, 68, y, label.toUpperCase(BRAZIL), 96, 112, 134);
    List<String> lines = wrap(safe(value), regular, size, maxWidth);
    float lineY = y;
    for (String line : lines) {
      text(content, regular, size, 210, lineY, line, 23, 43, 67);
      lineY -= 11;
    }
    float dividerY = Math.min(y - 15, lineY - 3);
    line(content, 68, dividerY, 527, dividerY, 238, 241, 244);
    return dividerY - 19;
  }

  private static void paragraph(
      PDPageContentStream content,
      PDFont font,
      float size,
      float x,
      float y,
      float maxWidth,
      String value,
      int red,
      int green,
      int blue)
      throws IOException {
    for (String line : wrap(value, font, size, maxWidth)) {
      text(content, font, size, x, y, line, red, green, blue);
      y -= 13;
    }
  }

  private static List<String> wrap(String value, PDFont font, float size, float maxWidth)
      throws IOException {
    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String word : value.split("\\s+")) {
      String candidate = current.isEmpty() ? word : current + " " + word;
      if (font.getStringWidth(candidate) / 1000f * size > maxWidth && !current.isEmpty()) {
        lines.add(current.toString());
        current = new StringBuilder(word);
      } else {
        if (!current.isEmpty()) current.append(' ');
        current.append(word);
      }
    }
    if (!current.isEmpty()) lines.add(current.toString());
    return lines;
  }

  private static void text(
      PDPageContentStream content,
      PDFont font,
      float size,
      float x,
      float y,
      String value,
      int red,
      int green,
      int blue)
      throws IOException {
    content.beginText();
    content.setFont(font, size);
    content.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
    content.newLineAtOffset(x, y);
    content.showText(safe(value));
    content.endText();
  }

  private static void fill(
      PDPageContentStream content,
      float x,
      float y,
      float width,
      float height,
      int red,
      int green,
      int blue)
      throws IOException {
    content.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
    content.addRect(x, y, width, height);
    content.fill();
  }

  private static void stroke(
      PDPageContentStream content,
      float x,
      float y,
      float width,
      float height,
      int red,
      int green,
      int blue)
      throws IOException {
    content.setStrokingColor(red / 255f, green / 255f, blue / 255f);
    content.setLineWidth(0.8f);
    content.addRect(x, y, width, height);
    content.stroke();
  }

  private static void line(
      PDPageContentStream content,
      float x1,
      float y1,
      float x2,
      float y2,
      int red,
      int green,
      int blue)
      throws IOException {
    content.setStrokingColor(red / 255f, green / 255f, blue / 255f);
    content.setLineWidth(0.5f);
    content.moveTo(x1, y1);
    content.lineTo(x2, y2);
    content.stroke();
  }

  private static String statusLabel(String status) {
    if ("approved".equalsIgnoreCase(status)) return "Pagamento aprovado";
    if ("refunded".equalsIgnoreCase(status)) return "Pagamento estornado";
    if ("pending".equalsIgnoreCase(status)) return "Pagamento pendente";
    return safe(status);
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
