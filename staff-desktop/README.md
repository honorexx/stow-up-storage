# StowUp HQ — Central Interna

App desktop (Windows e Mac) para a equipe da StowUp, organizado por departamento em vez de telas soltas por função. Cada funcionário entra com o código `SU123456` (ou e-mail) e vê apenas os módulos permitidos pelo seu papel.

Construído com **Electron + React + TypeScript + Vite**, consumindo a mesma API Spring Boot usada pelo app mobile.

## Departamentos

| Departamento | Quem acessa | Módulos |
| --- | --- | --- |
| Diretoria | `ADMIN` | Visão geral (dashboard) e auditoria |
| Atendimento | `ADMIN`, `SUPPORT` | Fila de chamados com chat |
| Operações | `ADMIN`, `MODERATOR`, `LISTING_ANALYST` | Moderação de anúncios |
| Jurídico | `ADMIN`, `MODERATOR`, `CONTRACT_ANALYST`, `DOCUMENT_ANALYST` | Contratos de locação e documentos de anúncio |
| RH | `ADMIN` | Equipe interna e usuários do app |
| Financeiro | `ADMIN` | Pagamentos (Mercado Pago) |

## Rodar em desenvolvimento

Pré-requisitos: Node.js 20+ e pnpm 11+. O backend Spring Boot precisa estar rodando (`http://localhost:8080` por padrão).

```bash
pnpm install
pnpm dev
```

Isso inicia o Vite (renderer) e abre a janela do Electron automaticamente, com DevTools.

Para apontar para outro backend, defina `VITE_API_URL` antes de `pnpm dev` ou `pnpm build`:

```bash
VITE_API_URL=https://api.stowup.com.br/api pnpm dev
```

## Gerar instalador

```bash
pnpm build:mac   # gera .dmg (executar em um Mac)
pnpm build:win   # gera instalador .exe (executar em um Windows)
```

Os artefatos ficam em `release/`. Cada plataforma precisa ser empacotada na própria plataforma (o `electron-builder` não faz cross-build de instalador nativo sem ferramentas extras).

## Estrutura

```text
staff-desktop/
├── electron/       Processo principal do Electron (main.js, preload.js)
├── src/
│   ├── api/        Cliente HTTP e tipos compartilhados com o backend
│   ├── state/       Autenticação (staff-login) e permissões por papel
│   ├── layout/       Shell, sidebar e definição dos departamentos
│   └── pages/        Uma pasta por departamento
└── package.json
```

## Qualidade

```bash
pnpm typecheck      # tsc --noEmit
pnpm format:check    # prettier --check
```
