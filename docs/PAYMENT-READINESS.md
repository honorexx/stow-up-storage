# StowUp — prontidão para pagamentos

## Estado atual

O projeto já possui uma separação hexagonal inicial para pagamentos:

- `PaymentService`: orquestra checkout e notificações;
- `PaymentGateway`: porta de saída para o provedor;
- `MercadoPagoPaymentGateway`: adaptador do Mercado Pago;
- `PaymentOrderStore`: porta de persistência;
- `JpaPaymentOrderStore`: adaptador PostgreSQL;
- `ContractPaymentPolicy`: impede pagamento antes da aprovação do contrato.

A revisão adicionou reaproveitamento de checkout recente, validação de valor e moeda retornados pelo provedor e confirmação idempotente da reserva.

## O que falta antes de produção

1. Escolher o modelo financeiro: pagamento integral para uma única conta ou marketplace com split entre proprietário e StowUp.
2. Para split, cadastrar cada proprietário no OAuth do Mercado Pago e armazenar tokens cifrados com renovação e revogação.
3. Informar `marketplace_fee` no Checkout Pro e registrar separadamente valor do aluguel, taxa da StowUp e tarifa do provedor.
4. Validar `x-signature`, `x-request-id` e `data.id` de todos os webhooks usando a chave secreta da aplicação.
5. Configurar uma URL HTTPS pública; `localhost` não funciona como `notification_url` ou `back_urls` em uma operação real.
6. Configurar deep links do aplicativo para sucesso, pendência e falha.
7. Implementar estorno, chargeback, cancelamento, conciliação e trilha de auditoria financeira.
8. Criar contas e cartões de teste e validar os cenários aprovado, pendente, rejeitado e reembolsado.

## Decisão recomendada

Para a StowUp como marketplace, a base recomendada é **Checkout Pro + Split de Pagamentos 1:1**. O comprador paga no ambiente do Mercado Pago, a plataforma não recebe dados completos do cartão e a comissão é informada na preferência. Cada proprietário precisa autorizar a StowUp por OAuth.

Referências oficiais:

- https://www.mercadopago.com.br/developers/pt/docs/checkout-pro/overview
- https://www.mercadopago.com.br/developers/pt/docs/checkout-pro/payment-notifications
- https://www.mercadopago.com.br/developers/pt/docs/split-payments/split-1-1/integration-configuration/integrate-marketplace
- https://www.mercadopago.com.br/developers/pt/docs/checkout-pro/configure-back-urls
