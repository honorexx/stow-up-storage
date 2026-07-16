package br.com.stowup.contract.adapter.out.pdf;

import br.com.stowup.contract.application.port.out.ContractDocumentGenerator;
import br.com.stowup.contract.domain.ContractData;
import java.io.IOException;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxContractDocumentGenerator implements ContractDocumentGenerator {
  private final Path root;

  public PdfBoxContractDocumentGenerator(
      @Value("${stowup.uploads.directory}") String uploadDirectory) {
    this.root = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("contracts");
  }

  @Override
  public Path generate(ContractData d) {
    try {
      Files.createDirectories(root);
      Path target = root.resolve(d.bookingId() + "-original.pdf");
      Path temporary = Files.createTempFile(root, d.bookingId() + "-", ".pdf.tmp");
      try (PDDocument document = new PDDocument()) {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        Writer out = new Writer(document, regular, bold);
        out.title("CONTRATO PARTICULAR DE LOCAÇÃO DE ESPAÇO PARA ARMAZENAMENTO");
        out.line(
            "Contrato "
                + d.contractCode()
                + "  |  Reserva "
                + d.bookingId()
                + "  |  Versão 2026-07-13",
            bold);
        out.notice("RASCUNHO PARA REVISÃO JURÍDICA ANTES DA OPERAÇÃO COMERCIAL.");
        out.section("1. IDENTIFICAÇÃO DAS PARTES");
        out.paragraph(
            "LOCADOR / ANUNCIANTE: "
                + d.ownerName()
                + ", e-mail "
                + d.ownerEmail()
                + ". LOCATÁRIO / CLIENTE: "
                + d.customerName()
                + ", e-mail "
                + d.customerEmail()
                + ".");
        out.section("2. OBJETO E ESPAÇO");
        out.paragraph(
            "O LOCADOR disponibiliza ao LOCATÁRIO o espaço "
                + d.spaceTitle()
                + ", tipo "
                + d.spaceType()
                + ", com área anunciada de "
                + d.spaceSize()
                + " m², situado em "
                + d.spaceAddress()
                + ", exclusivamente para armazenamento lícito e compatível com a estrutura anunciada.");
        out.section("3. PRAZO, PREÇO E PAGAMENTO");
        out.paragraph(
            "A locação inicia em "
                + d.startDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ", por "
                + d.months()
                + " mês(es), no total de "
                + NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(d.total())
                + ". O pagamento é processado pelo Mercado Pago; a StowUp não armazena o número completo do cartão ou CVV.");
        out.section("4. RESPONSABILIDADE E LIMITES DA INTERMEDIAÇÃO");
        out.paragraph(
            "A StowUp atua como plataforma de intermediação, pagamento, suporte e gestão documental e não recebe, transporta, manuseia ou guarda fisicamente os bens. Não responde por perda, extravio, furto, avaria, quebra ou deterioração decorrente exclusivamente de ato ou omissão do LOCADOR ou LOCATÁRIO, da natureza ou embalagem dos bens, de terceiro, caso fortuito externo ou força maior, quando não houver falha comprovada do serviço da StowUp. Permanecem as responsabilidades que a lei não permite excluir e as decorrentes de falha da própria plataforma.");
        out.section("5. ITENS PROIBIDOS");
        out.paragraph(
            "É proibido armazenar bens ilícitos, roubados, explosivos, drogas ilícitas, armas em desacordo com a lei, resíduos perigosos, animais, pessoas, perecíveis sem estrutura adequada, dinheiro em espécie ou qualquer item que ameace pessoas, patrimônio ou meio ambiente.");
        out.pageBreak();
        out.section("6. INADIMPLÊNCIA E RETIRADA");
        out.paragraph(
            "Em atraso, o LOCATÁRIO será notificado para regularizar e retirar os bens, quando cabível, em prazo operacional de até 15 dias após notificação válida. O prazo não transfere automaticamente a propriedade dos bens. Retenção, remoção, descarte ou alienação dependem de procedimento juridicamente permitido e documentado.");
        out.section("7. ASSINATURA ELETRÔNICA DO LOCATÁRIO");
        out.paragraph(
            "Somente o LOCATÁRIO assina este contrato pelo gov.br. O LOCADOR já aderiu às obrigações de guarda, segurança e comunicação de incidentes no Termo de Responsabilidade do Anunciante vinculado ao espaço. O PDF original assinado pelo LOCATÁRIO deve ser enviado à StowUp, sem impressão ou conversão, para validação de autenticidade, integridade e nome no VALIDAR/ITI.");
        out.section("8. CONSENTIMENTO DO LOCATÁRIO");
        out.paragraph(
            "O LOCATÁRIO declara que leu, conferiu seus dados, concorda com as condições e se compromete a não armazenar itens ilícitos, perigosos ou incompatíveis com o espaço anunciado.");
        out.signature(d.customerName(), "LOCATÁRIO / CLIENTE");
        out.finish();
        document.save(temporary.toFile());
      }
      try {
        Files.move(
            temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return target;
    } catch (IOException e) {
      throw new IllegalStateException("Não foi possível gerar o contrato PDF.", e);
    }
  }

  private static final class Writer {
    private final PDDocument doc;
    private final PDFont regular;
    private final PDFont bold;
    private PDPage page;
    private PDPageContentStream stream;
    private float y;

    Writer(PDDocument doc, PDFont regular, PDFont bold) throws IOException {
      this.doc = doc;
      this.regular = regular;
      this.bold = bold;
      newPage();
    }

    void newPage() throws IOException {
      if (stream != null) stream.close();
      page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      stream = new PDPageContentStream(doc, page);
      stream.setNonStrokingColor(0f, 43f / 255f, 92f / 255f);
      stream.addRect(0, 800, 595, 42);
      stream.fill();
      text("STOWUP", 48, 816, 12, bold, 255, 255, 255);
      y = 760;
    }

    void title(String value) throws IOException {
      for (String line : wrap(value, bold, 15, 485)) {
        text(line, 55, y, 15, bold, 23, 43, 67);
        y -= 21;
      }
      y -= 8;
    }

    void notice(String value) throws IOException {
      paragraph(value);
    }

    void pageBreak() throws IOException {
      newPage();
    }

    void section(String value) throws IOException {
      ensure(50);
      y -= 7;
      text(value, 55, y, 11, bold, 0, 43, 92);
      y -= 18;
    }

    void paragraph(String value) throws IOException {
      for (String line : wrap(value, regular, 9, 485)) {
        ensure(18);
        text(line, 55, y, 9, regular, 23, 43, 67);
        y -= 14;
      }
      y -= 4;
    }

    void line(String value, PDFont font) throws IOException {
      ensure(20);
      text(value, 55, y, 9, font, 96, 112, 134);
      y -= 19;
    }

    void signature(String name, String role) throws IOException {
      ensure(62);
      y -= 25;
      line("____________________________________________", regular);
      line(name + " - " + role, regular);
    }

    void ensure(float required) throws IOException {
      if (y - required < 60) newPage();
    }

    void finish() throws IOException {
      if (stream != null) stream.close();
    }

    void text(String value, float x, float y, float size, PDFont font, int r, int g, int b)
        throws IOException {
      stream.beginText();
      stream.setFont(font, size);
      stream.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
      stream.newLineAtOffset(x, y);
      stream.showText(asWinAnsi(value));
      stream.endText();
    }

    List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {
      List<String> lines = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      for (String word : value.split(" ")) {
        String candidate = current.isEmpty() ? word : current + " " + word;
        float width = font.getStringWidth(asWinAnsi(candidate)) / 1000f * size;
        if (width > maxWidth && !current.isEmpty()) {
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

    String asWinAnsi(String value) {
      return value.replace("–", "-").replace("—", "-");
    }
  }
}
