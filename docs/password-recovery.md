# Recuperação de senha

O fluxo usa um código de seis dígitos, válido por 10 minutos, enviado somente ao e-mail ou ao telefone que já pertence à conta. O reenvio é liberado após 30 segundos e o código é bloqueado depois de cinco tentativas incorretas.

## Desenvolvimento local

Por padrão, `PASSWORD_RECOVERY_DELIVERY_MODE=log`. O código não é devolvido pela API: ele aparece apenas no log protegido do Spring Boot, identificado por `MODO LOCAL - código de recuperação StowUp`.

## Envio real

Defina `PASSWORD_RECOVERY_DELIVERY_MODE=live` e configure as variáveis do canal que será usado.

### E-mail via SMTP

- `PASSWORD_RECOVERY_EMAIL_FROM`
- `SMTP_HOST`
- `SMTP_PORT` (padrão: `587`)
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `SMTP_AUTH` (padrão: `true`)
- `SMTP_STARTTLS` (padrão: `true`)

### SMS via Twilio

- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_FROM_NUMBER` em formato internacional E.164

Nunca coloque essas credenciais no Git. Em produção, use os segredos do provedor de hospedagem.
