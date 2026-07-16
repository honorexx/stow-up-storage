from pathlib import Path
import shutil
from reportlab.lib import colors
from reportlab.lib.enums import TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "legal" / "TERMOS_DE_USO_RASCUNHO.md"
OUTPUT = ROOT / "output" / "pdf" / "termos-de-uso-2026-07-13.pdf"
STATIC = ROOT / "src" / "main" / "resources" / "static" / "legal" / OUTPUT.name
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
STATIC.parent.mkdir(parents=True, exist_ok=True)

NAVY = colors.HexColor("#002B5C")
ORANGE = colors.HexColor("#FF5B00")
TEXT = colors.HexColor("#172B43")
MUTED = colors.HexColor("#607086")
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="TermsTitle", fontName="Helvetica-Bold", fontSize=22, leading=27, textColor=NAVY, spaceAfter=8))
styles.add(ParagraphStyle(name="TermsSection", fontName="Helvetica-Bold", fontSize=12, leading=16, textColor=NAVY, spaceBefore=13, spaceAfter=6))
styles.add(ParagraphStyle(name="TermsBody", fontName="Helvetica", fontSize=9, leading=14, textColor=TEXT, alignment=TA_JUSTIFY, spaceAfter=7))
styles.add(ParagraphStyle(name="TermsNotice", fontName="Helvetica-Bold", fontSize=8.5, leading=13, textColor=colors.HexColor("#8A3B00"), backColor=colors.HexColor("#FFF4E8"), borderPadding=8, spaceAfter=12))


def header_footer(canvas, doc):
    canvas.saveState(); width, height = A4
    canvas.setFillColor(colors.white); canvas.rect(0, 0, width, height, stroke=0, fill=1)
    canvas.setFillColor(NAVY); canvas.rect(0, height - 13 * mm, width, 13 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.white); canvas.setFont("Helvetica-Bold", 10); canvas.drawString(18 * mm, height - 8.4 * mm, "STOWUP")
    canvas.setFillColor(ORANGE); canvas.circle(width - 18 * mm, height - 7 * mm, 1.6 * mm, stroke=0, fill=1)
    canvas.setStrokeColor(colors.HexColor("#DCE3EA")); canvas.line(18 * mm, 15 * mm, width - 18 * mm, 15 * mm)
    canvas.setFillColor(MUTED); canvas.setFont("Helvetica", 7); canvas.drawString(18 * mm, 10 * mm, "Termos de Uso - versão 2026-07-13"); canvas.drawRightString(width - 18 * mm, 10 * mm, f"Página {doc.page}")
    canvas.restoreState()


story = [Spacer(1, 5 * mm)]
for raw in SOURCE.read_text(encoding="utf-8").splitlines():
    line = raw.strip()
    if not line:
        continue
    if line.startswith("# "):
        story.append(Paragraph(line[2:], styles["TermsTitle"]))
    elif line.startswith("## "):
        if line.startswith("## 8."):
            story.append(PageBreak())
        story.append(Paragraph(line[3:], styles["TermsSection"]))
    elif line.startswith("> "):
        story.append(Paragraph(line[2:], styles["TermsNotice"]))
    elif line.startswith("Versão:"):
        story.append(Paragraph(line, ParagraphStyle(name="Version", parent=styles["TermsBody"], textColor=ORANGE, fontName="Helvetica-Bold")))
    else:
        story.append(Paragraph(line, styles["TermsBody"]))

doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=20 * mm, leftMargin=20 * mm, topMargin=20 * mm, bottomMargin=20 * mm, title="Termos de Uso StowUp", author="StowUp")
doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
shutil.copyfile(OUTPUT, STATIC)
print(OUTPUT)
