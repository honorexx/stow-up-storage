package br.com.stowup.listingdocument.adapter.out.pdf;

import br.com.stowup.listingdocument.application.port.out.ListingDocumentGenerator;
import br.com.stowup.space.Space;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxListingDocumentGenerator implements ListingDocumentGenerator {
  private final Path root;

  public PdfBoxListingDocumentGenerator(@Value("${stowup.uploads.directory}") String directory) {
    root = Path.of(directory).toAbsolutePath().normalize().resolve("listing-documents");
  }

  @Override
  public Path generate(Space space) {
    try {
      Files.createDirectories(root);
      Path target = root.resolve("space-" + space.getId() + "-original.pdf");
      Path temporary = Files.createTempFile(root, "space-" + space.getId() + "-", ".pdf.tmp");
      try (PDDocument doc = new PDDocument()) {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        Writer out = new Writer(doc, regular, bold);
        out.title("TERMO DE RESPONSABILIDADE DO ANUNCIANTE E DISPONIBILIZAÇÃO DE ESPAÇO");
        out.line(
            "Documento DOC-SU-"
                + space.getId()
                + " | Anúncio "
                + space.getId()
                + " | Versão 2026-07-13",
            bold);
        out.section("1. ANUNCIANTE E ESPAÇO");
        out.paragraph(
            "ANUNCIANTE: "
                + space.getOwner().getName()
                + ", e-mail "
                + space.getOwner().getEmail()
                + ". ESPAÇO: "
                + space.getTitle()
                + ", "
                + space.getType()
                + " de "
                + space.getSize()
                + " m², situado em "
                + address(space)
                + ".");
        out.section("2. DECLARAÇÃO DE LEGITIMIDADE");
        out.paragraph(
            "O ANUNCIANTE declara possuir legitimidade para oferecer o espaço, que as informações, medidas, fotos, condições de acesso e recursos de segurança são verdadeiros e que comunicará imediatamente qualquer mudança relevante.");
        out.section("3. DEVER DE GUARDA E SEGURANÇA");
        out.paragraph(
            "Enquanto houver bens de um locatário no espaço, o ANUNCIANTE compromete-se a cumprir o contrato de locação, preservar as condições anunciadas, controlar o acesso, empregar cuidado razoável na guarda e comunicar incidentes à StowUp e ao locatário. A responsabilidade por perdas ou danos será apurada conforme o contrato, as evidências e a legislação aplicável.");
        out.section("4. PRIVACIDADE, ACESSO E INCIDENTES");
        out.paragraph(
            "O ANUNCIANTE não poderá abrir, usar, remover, reter ou divulgar os bens fora das hipóteses legalmente permitidas. Suspeitas de itens ilícitos, riscos, avarias, invasão ou inadimplência deverão seguir o procedimento documentado da StowUp e a legislação, sem apropriação automática dos bens.");
        out.section("5. ASSINATURA E ANÁLISE");
        out.paragraph(
            "O PDF deve ser conferido e assinado eletronicamente pelo gov.br. O arquivo original assinado será enviado à StowUp e analisado no VALIDAR/ITI. O anúncio somente poderá ser publicado depois da aprovação documental e da análise das informações e fotos do espaço.");
        out.signature(space.getOwner().getName(), "ANUNCIANTE / RESPONSÁVEL PELO ESPAÇO");
        out.finish();
        doc.save(temporary.toFile());
      }
      try {
        Files.move(
            temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return target;
    } catch (IOException e) {
      throw new IllegalStateException("Não foi possível gerar o termo do anunciante.", e);
    }
  }

  private static String address(Space s) {
    return s.getAddressLine()
        + ", "
        + s.getAddressNumber()
        + ", "
        + s.getDistrict()
        + ", "
        + s.getCity()
        + "/"
        + s.getState();
  }

  private static final class Writer {
    private final PDDocument doc;
    private final PDFont regular;
    private final PDFont bold;
    private PDPageContentStream stream;
    private float y;

    Writer(PDDocument doc, PDFont regular, PDFont bold) throws IOException {
      this.doc = doc;
      this.regular = regular;
      this.bold = bold;
      page();
    }

    void page() throws IOException {
      if (stream != null) stream.close();
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      stream = new PDPageContentStream(doc, page);
      stream.setNonStrokingColor(0f, 43f / 255f, 92f / 255f);
      stream.addRect(0, 800, 595, 42);
      stream.fill();
      text("STOWUP", 48, 816, 12, bold, 255, 255, 255);
      y = 760;
    }

    void title(String v) throws IOException {
      for (String l : wrap(v, 50)) {
        text(l, 55, y, 15, bold, 23, 43, 67);
        y -= 21;
      }
      y -= 8;
    }

    void section(String v) throws IOException {
      ensure(45);
      y -= 6;
      text(v, 55, y, 11, bold, 0, 43, 92);
      y -= 18;
    }

    void paragraph(String v) throws IOException {
      for (String l : wrap(v, 100)) {
        ensure(18);
        text(l, 55, y, 9, regular, 23, 43, 67);
        y -= 14;
      }
      y -= 4;
    }

    void line(String v, PDFont f) throws IOException {
      ensure(20);
      text(v, 55, y, 9, f, 96, 112, 134);
      y -= 19;
    }

    void signature(String n, String r) throws IOException {
      ensure(70);
      y -= 30;
      line("____________________________________________", regular);
      line(n + " - " + r, regular);
    }

    void ensure(float n) throws IOException {
      if (y - n < 60) page();
    }

    void finish() throws IOException {
      if (stream != null) stream.close();
    }

    void text(String v, float x, float yy, float size, PDFont f, int r, int g, int b)
        throws IOException {
      stream.beginText();
      stream.setFont(f, size);
      stream.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
      stream.newLineAtOffset(x, yy);
      stream.showText(v.replace("–", "-").replace("—", "-"));
      stream.endText();
    }

    List<String> wrap(String v, int max) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (String w : v.split(" ")) {
        if (line.length() + w.length() + 1 > max) {
          out.add(line.toString());
          line = new StringBuilder();
        }
        if (!line.isEmpty()) line.append(' ');
        line.append(w);
      }
      if (!line.isEmpty()) out.add(line.toString());
      return out;
    }
  }
}
