# StowUp — API de marketplace

Todas as rotas abaixo usam o prefixo `/api`. Exceto busca pública, detalhes dos espaços, termos legais e calendário de disponibilidade, as rotas exigem um token JWT em `Authorization: Bearer <token>`.

## Busca e disponibilidade

- `GET /spaces`: busca por texto, cidade, bairro, CEP, preço, tamanho, tipo, características e ordenação.
- `GET /spaces/{id}`: detalhes públicos do espaço.
- `GET /bookings/availability/{spaceId}`: intervalos já ocupados.
- `POST /bookings`: cria uma solicitação e bloqueia conflito de datas.

## Experiência do cliente

- `GET|POST|DELETE /favorites`: consulta, adiciona e remove favoritos.
- `GET /bookings?category=ACTIVE`: organiza contratos/reservas por categoria.
- `POST /bookings/{id}/checkout`: confirma a retirada dos itens.
- `GET /reviews/eligible`: locações encerradas que ainda estão na janela de 14 dias.
- `POST /reviews`: envia a avaliação do proprietário ou do cliente.
- `GET /notifications` e `PATCH /notifications/{id}/read`: central de notificações.

## Proprietário

- `GET /owner/dashboard`: ocupação, reservas, contratos e receitas.
- `GET /owner/financial-report`: receita e histórico financeiro.
- `GET /owner/rental-history`: histórico das locações dos espaços.
- `PATCH /owner/spaces/{id}/availability`: suspensão temporária e reativação.
- `GET|POST /owner/spaces/{id}/documents`: documentos complementares.

## Administração

- `GET /admin/dashboard`: visão consolidada da operação.
- `GET /admin/audit`: últimas alterações relevantes.
- `POST /admin/spaces/{id}/approve|reject|suspend`: ciclo administrativo do anúncio.
- `GET /admin/spaces/{id}/documents`: documentos complementares do espaço.
- `GET /admin/space-documents/{id}/download`: download controlado de documento.

O mínimo de fotos é configurado por `STOWUP_MINIMUM_SPACE_PHOTOS` (padrão: `4`).
