# StowUp

Marketplace de espaços para armazenamento com aplicativo Expo responsivo para iOS, Android e web/desktop, API Spring Boot e PostgreSQL. A Central Administrativa é uma aplicação web interna separada do app/site público, com login próprio em `/staff` e APIs protegidas por função. Os módulos principais seguem arquitetura hexagonal, separando domínio, casos de uso, portas e adaptadores.

## Clonar e executar no Windows

Pré-requisitos: Windows 10/11, conexão com a internet e Expo Go compatível com SDK 54 no celular. Java, Maven, Node.js, pnpm e PostgreSQL são detectados ou preparados localmente pelo instalador, sem exigir instalação global.

```powershell
git clone URL_DO_REPOSITORIO
cd StowUp
```

Depois, execute com duplo clique:

1. `INSTALAR.bat` — prepara ferramentas, banco, dependências, IP local e compila os projetos.
2. `INICIAR.bat` — inicia PostgreSQL, Spring Boot e Expo. O app público não exibe atalhos administrativos; a equipe acessa a central diretamente em `http://localhost:8080/staff`.
3. `PARAR.bat` — encerra o ambiente local.

O primeiro processo pode demorar porque baixa as ferramentas e dependências. Tudo que é específico da máquina fica em `.runtime/`, `postgres-data/`, `target/`, `mobile/node_modules/` e `mobile/.env.local`; essas pastas não entram no Git.

As credenciais aleatórias do usuário mestre são criadas em:

```text
.runtime/ACESSO_LOCAL.txt
```

O computador e o celular precisam estar na mesma rede. O instalador detecta o IP do computador e grava `EXPO_PUBLIC_API_URL` em `mobile/.env.local`, conforme o padrão de variáveis públicas do Expo.

Execute apenas uma instalação local do StowUp por vez. Se outra cópia já estiver usando as portas 5432 ou 8080, os scripts interrompem a inicialização com uma mensagem clara; use `PARAR.bat` na outra cópia antes de continuar.

## Abrir pelo IntelliJ

- Abra a pasta raiz para editar e executar o Spring Boot.
- A aplicação pública está em `mobile/`.
- A **Central Interna** (app desktop para funcionários) está em `staff-desktop/` — Electron + React + TypeScript, instalável no PC/Mac. Execute com `pnpm install` e `pnpm dev` dentro da pasta.
- O atendimento web leve legado continua em `src/main/resources/static/staff/`.
- As configurações compartilhadas ficam em `.run/`: `StowUp — API + Site`, `StowUp — App Mobile`, `StowUp — App Web` e `StowUp — Tudo (Web)`.
- Para o fluxo reproduzível usado por todos os colaboradores, prefira `INSTALAR.bat` e `INICIAR.bat`.

## Estrutura

```text
StowUp/
├── mobile/          React Native + Expo SDK 54 (iOS, Android e web público)
├── staff-desktop/   Central Interna Electron + React (app desktop para funcionários)
├── src/             API Spring Boot e aplicação web
├── docs/            API, arquitetura, contratos e operação
├── tools/           Bootstrap, banco e comandos de execução
├── INSTALAR.bat
├── INICIAR.bat
└── PARAR.bat
```

## E-mail de recuperação de senha

Sem credenciais, o ambiente inicia em modo local e os códigos aparecem apenas no log protegido do backend. Para configurar envio real em cada computador:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\configure-recovery-email.ps1
```

O script solicita o remetente e a senha de aplicativo diretamente no terminal, salva a senha com proteção DPAPI do Windows e reinicia o backend. Esses dados nunca entram no Git. Consulte [docs/password-recovery.md](docs/password-recovery.md).

O envio por SMS exige credenciais de um provedor, atualmente preparado para Twilio.

Depois de criar uma conta/número na Twilio, configure o envio real de SMS com:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\configure-recovery-sms.ps1
```

## Configuração de produção

Use [.env.example](.env.example) como referência. Nunca publique `.env`, `.env.local`, `.runtime`, senhas SMTP, tokens da Twilio, chaves do Mercado Pago, segredos JWT ou chaves de criptografia.

Para pagamentos reais, configure:

```text
MERCADO_PAGO_PUBLIC_KEY
MERCADO_PAGO_ACCESS_TOKEN
STOWUP_PUBLIC_URL=https://seu-backend-publico.example
```

O endereço público precisa usar HTTPS e aceitar webhooks. O sistema não armazena número completo de cartão nem CVV.

Antes de usar dinheiro real, salve e valide as credenciais de teste com:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\configure-mercado-pago.ps1
```

## Funcionalidades principais

- cadastro, login, recuperação de senha, perfil, CPF cifrado, telefone, CEP e foto;
- busca, mapa, filtros, favoritos, distância e calendário de disponibilidade;
- anúncios, fotos, documentos, moderação, suspensão e histórico;
- reservas, contratos PDF, assinatura externa, pagamentos e check-out;
- painel e relatórios do proprietário;
- notificações, avaliações pós-locação e conversas privadas;
- chamados de suporte com histórico e chat;
- central administrativa interna separada, login próprio e autorização de APIs por função.

## Testes

Backend:

```powershell
.\tools\mvn.cmd spotless:check test
```

Mobile:

```powershell
cd mobile
..\tools\pnpm.cmd install --frozen-lockfile
..\tools\pnpm.cmd exec tsc --noEmit
..\tools\pnpm.cmd run format:check
```

O GitHub Actions executa essas verificações automaticamente em cada push e pull request.

## Publicar no GitHub

Crie no GitHub um repositório vazio, sem adicionar README ou `.gitignore` pelo site. Na pasta raiz do StowUp, execute:

```powershell
git init
git add .
git commit -m "Versão inicial do StowUp"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/stowup.git
git push -u origin main
```

Depois, cada colaborador poderá usar:

```powershell
git clone https://github.com/SEU_USUARIO/stowup.git
cd stowup
.\INSTALAR.bat
```

O ZIP da pasta `output/` também pode ser usado para criar o primeiro repositório, mas essa pasta é ignorada pelo Git e não será enviada nos commits.

## Avisos

Os contratos e Termos de Uso são rascunhos técnicos e precisam de revisão jurídica antes do uso comercial. Credenciais locais, dados pessoais, banco, uploads e documentos enviados por usuários não devem ser publicados no repositório.
