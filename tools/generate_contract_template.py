from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "contrato_locacao_stowup_modelo.pdf"
OUTPUT.parent.mkdir(parents=True, exist_ok=True)

NAVY = colors.HexColor("#002B5C")
ORANGE = colors.HexColor("#FF5B00")
TEXT = colors.HexColor("#172B43")
MUTED = colors.HexColor("#607086")
LINE = colors.HexColor("#DCE3EA")
LIGHT = colors.HexColor("#F5F7FA")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="Brand", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=25, leading=29, textColor=NAVY, spaceAfter=2))
styles.add(ParagraphStyle(name="DocTitle", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=15, leading=19, textColor=TEXT, alignment=TA_CENTER, spaceAfter=13))
styles.add(ParagraphStyle(name="Section", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=10.5, leading=14, textColor=NAVY, spaceBefore=12, spaceAfter=6))
styles.add(ParagraphStyle(name="BodyLegal", parent=styles["BodyText"], fontName="Helvetica", fontSize=8.6, leading=13, textColor=TEXT, alignment=TA_JUSTIFY, spaceAfter=6))
styles.add(ParagraphStyle(name="Small", parent=styles["BodyText"], fontName="Helvetica", fontSize=7.5, leading=10, textColor=MUTED))
styles.add(ParagraphStyle(name="Notice", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=8, leading=11, textColor=colors.HexColor("#8A3B00")))


def header_footer(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(colors.white)
    canvas.rect(0, 0, width, height, stroke=0, fill=1)
    canvas.setFillColor(NAVY)
    canvas.rect(0, height - 13 * mm, width, 13 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.white)
    canvas.setFont("Helvetica-Bold", 10)
    canvas.drawString(18 * mm, height - 8.4 * mm, "STOWUP")
    canvas.setFillColor(ORANGE)
    canvas.circle(width - 18 * mm, height - 7 * mm, 1.6 * mm, stroke=0, fill=1)
    canvas.setStrokeColor(LINE)
    canvas.line(18 * mm, 15 * mm, width - 18 * mm, 15 * mm)
    canvas.setFillColor(MUTED)
    canvas.setFont("Helvetica", 7)
    canvas.drawString(18 * mm, 10 * mm, "Contrato gerado eletronicamente pela plataforma StowUp")
    canvas.drawRightString(width - 18 * mm, 10 * mm, f"Página {doc.page}")
    canvas.restoreState()


def p(text, style="BodyLegal"):
    return Paragraph(text, styles[style])


story = [
    Spacer(1, 5 * mm),
    p("Stow<span color='#FF5B00'>Up</span>", "Brand"),
    p("SEU ESPAÇO, QUANDO PRECISAR.", "Small"),
    Spacer(1, 7 * mm),
    p("CONTRATO PARTICULAR DE LOCAÇÃO DE ESPAÇO PARA ARMAZENAMENTO", "DocTitle"),
    Table([
        [p("Código do contrato", "Small"), p("Reserva", "Small"), p("Versão", "Small")],
        [p("<b>CTR-ABC123</b>"), p("<b>RSV-ABC123</b>"), p("<b>2026-07-13</b>")],
    ], colWidths=[55 * mm, 55 * mm, 45 * mm], style=TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), LIGHT), ("BOX", (0, 0), (-1, -1), .6, LINE),
        ("INNERGRID", (0, 0), (-1, -1), .4, LINE), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ])),
    p("RASCUNHO PARA REVISÃO JURÍDICA. Os campos e cláusulas deste modelo devem ser revisados por profissional habilitado antes da operação comercial.", "Notice"),
    p("1. IDENTIFICAÇÃO DAS PARTES", "Section"),
    Table([
        [p("LOCADOR / ANUNCIANTE", "Small"), p("LOCATÁRIO / CLIENTE", "Small")],
        [p("Nome: {{OWNER_NAME}}<br/>CPF: {{OWNER_CPF_MASKED}}<br/>E-mail: {{OWNER_EMAIL}}<br/>Endereço: {{OWNER_ADDRESS}}"),
         p("Nome: {{CUSTOMER_NAME}}<br/>CPF: {{CUSTOMER_CPF_MASKED}}<br/>E-mail: {{CUSTOMER_EMAIL}}<br/>Endereço: {{CUSTOMER_ADDRESS}}")],
    ], colWidths=[77.5 * mm, 77.5 * mm], style=TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), LIGHT), ("BOX", (0, 0), (-1, -1), .6, LINE),
        ("INNERGRID", (0, 0), (-1, -1), .4, LINE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8), ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ])),
    p("2. OBJETO E ESPAÇO", "Section"),
    p("2.1. O LOCADOR disponibiliza ao LOCATÁRIO o espaço identificado como <b>{{SPACE_TITLE}}</b>, do tipo {{SPACE_TYPE}}, com área anunciada de {{SPACE_SIZE}} m², situado em {{SPACE_ADDRESS}}, exclusivamente para armazenamento lícito e compatível com a estrutura anunciada."),
    p("2.2. As condições de acesso, segurança, horários, limitações, fotos e características declaradas no anúncio integram este contrato. Divergências relevantes devem ser registradas no suporte antes do depósito dos bens."),
    p("3. PRAZO, PREÇO E PAGAMENTO", "Section"),
    p("3.1. A locação inicia em {{START_DATE}}, pelo prazo inicial de {{MONTHS}} mês(es). O valor total apresentado para o período é de <b>R$ {{TOTAL}}</b>, incluindo as taxas informadas antes da contratação."),
    p("3.2. O pagamento será processado pelo provedor indicado pela StowUp. A plataforma não armazena o número completo do cartão nem o código de segurança. A confirmação da reserva depende da aprovação do pagamento e da validação documental aplicável."),
    PageBreak(),
    p("4. OBRIGAÇÕES DO LOCATÁRIO", "Section"),
    p("4.1. O LOCATÁRIO deve utilizar o espaço com cuidado, respeitar limites, acesso e segurança, manter os dados cadastrais atualizados, pagar pontualmente e retirar os bens ao término ou quando houver obrigação contratual de desocupação."),
    p("4.2. É proibido armazenar bens ilícitos, roubados, explosivos, inflamáveis sem autorização e estrutura adequada, drogas ilícitas, armas ou munições em desacordo com a lei, resíduos perigosos, animais, pessoas, perecíveis sem conservação apropriada, dinheiro em espécie, documentos ao portador ou itens capazes de ameaçar pessoas, patrimônio ou meio ambiente."),
    p("4.3. O LOCATÁRIO declara ser proprietário ou legítimo possuidor dos bens armazenados e responde por sua origem, acondicionamento, legalidade e compatibilidade com o espaço."),
    p("5. OBRIGAÇÕES DO LOCADOR", "Section"),
    p("5.1. O LOCADOR deve manter o espaço nas condições anunciadas, permitir o acesso conforme combinado, informar riscos ou restrições, preservar a privacidade e não remover, abrir ou manipular os bens sem base contratual, emergência comprovada ou autorização legal."),
    p("6. INADIMPLÊNCIA, NOTIFICAÇÃO E RETIRADA", "Section"),
    p("6.1. Em caso de atraso, o LOCATÁRIO será notificado pelos meios cadastrados para regularizar a dívida e, quando cabível, retirar os bens. O contrato poderá prever prazo operacional de até 15 dias após notificação válida, sem afastar prazos ou direitos obrigatórios previstos em lei."),
    p("6.2. O decurso do prazo não transfere automaticamente a propriedade dos bens ao LOCADOR ou à StowUp. Retenção, remoção, depósito, descarte ou alienação somente poderão ocorrer quando juridicamente permitidos, mediante procedimento documentado, preservação de evidências, notificações e oportunidade de defesa ou retirada."),
    p("7. SEGURANÇA, INCIDENTES E SUPORTE", "Section"),
    p("7.1. Suspeita de item proibido, fraude, invasão, dano, ameaça ou emergência deverá ser comunicada imediatamente pelo chamado da StowUp e, quando necessário, às autoridades. A plataforma poderá preservar registros, restringir acessos e suspender a operação enquanto avalia o risco."),
    p("8. CANCELAMENTO E ENCERRAMENTO", "Section"),
    p("8.1. Cancelamentos, reembolsos, multas e devoluções seguirão as condições exibidas na contratação, este contrato e a legislação aplicável. Ao término, o espaço deverá ser devolvido desocupado e no estado compatível com o uso regular."),
    p("9. PROTEÇÃO DE DADOS", "Section"),
    p("9.1. Dados pessoais serão tratados para identidade, contrato, pagamento, prevenção à fraude, suporte, segurança e obrigações legais. Documentos e registros terão acesso restrito conforme a função do usuário interno."),
    PageBreak(),
    p("10. ASSINATURA ELETRÔNICA E VALIDAÇÃO", "Section"),
    p("10.1. As partes poderão assinar este PDF eletronicamente pelo serviço gov.br ou por outro meio admitido. O arquivo original assinado deverá ser enviado sem impressão, digitalização ou conversão posterior que prejudique a assinatura."),
    p("10.2. A equipe autorizada verificará autenticidade e integridade no serviço oficial VALIDAR do Instituto Nacional de Tecnologia da Informação. A validação técnica não substitui a análise do conteúdo do contrato nem garante fatos externos ao certificado."),
    p("11. DISPOSIÇÕES FINAIS", "Section"),
    p("11.1. Comunicações registradas na plataforma, comprovantes e versões do contrato integram o histórico da locação. A eventual invalidade de uma cláusula não invalida as demais. Permanecem preservados os direitos obrigatórios previstos na legislação aplicável."),
    Spacer(1, 10 * mm),
    p("DECLARAÇÃO DE CONSENTIMENTO", "Section"),
    p("As partes declaram que leram o documento, tiveram oportunidade de esclarecer dúvidas, conferiram seus dados e concordam com as condições da locação e com a proibição de armazenamento de itens ilícitos ou perigosos."),
    Spacer(1, 13 * mm),
    Table([
        [p("________________________________________", "BodyLegal"), p("________________________________________", "BodyLegal")],
        [p("{{OWNER_NAME}}<br/>LOCADOR / ANUNCIANTE", "Small"), p("{{CUSTOMER_NAME}}<br/>LOCATÁRIO / CLIENTE", "Small")],
    ], colWidths=[77.5 * mm, 77.5 * mm], style=TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("ALIGN", (0, 0), (-1, -1), "CENTER")])),
    Spacer(1, 8 * mm),
    Table([[p("Validação interna", "Small"), p("Resultado VALIDAR/ITI", "Small"), p("Data", "Small")], [p("{{REVIEWER}}"), p("{{VALIDATION_RESULT}}"), p("{{REVIEWED_AT}}")]], colWidths=[58 * mm, 58 * mm, 39 * mm], style=TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), LIGHT), ("BOX", (0, 0), (-1, -1), .6, LINE), ("INNERGRID", (0, 0), (-1, -1), .4, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7), ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ])),
]

doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=20 * mm, leftMargin=20 * mm, topMargin=20 * mm, bottomMargin=20 * mm, title="Contrato de Locação StowUp - Modelo", author="StowUp")
doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
print(OUTPUT)
