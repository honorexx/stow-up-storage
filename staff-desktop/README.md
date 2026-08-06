# StowUp Staff Desktop

Aplicação JavaFX integrada ao mesmo Spring Boot e PostgreSQL usados pelo app dos clientes. Cada funcionário entra com o próprio acesso e recebe uma tela conforme sua função:

- `ADMIN`: usuário mestre, com visão geral da plataforma, acesso total e gestão da equipe;
- `SUPPORT`: atendimento, chamados e conversas;
- `LISTING_ANALYST`: análise e aprovação de anúncios;
- `DOCUMENT_ANALYST`: termo assinado e documentação do anunciante;
- `CONTRACT_ANALYST`: contratos de locação entre cliente e anunciante.

O instalador cria o primeiro acesso mestre com senha aleatória em `.runtime/ACESSO_LOCAL.txt`. Esse arquivo é exclusivo da máquina e não entra no Git. Em produção, configure `STOWUP_MASTER_EMAIL` e `STOWUP_MASTER_PASSWORD` pelo gerenciador de segredos da hospedagem.

Ao cadastrar um funcionário, a central cria automaticamente um identificador no formato `SU123456`. O funcionário pode entrar com esse código ou com o e-mail, usando a senha temporária definida pelo mestre. As filas do JavaFX são sincronizadas automaticamente a cada 10 segundos.

## Abrir no IntelliJ

1. `File > Open` e selecione a pasta `staff-desktop`.
2. Aguarde o Maven sincronizar as dependências.
3. Inicie antes o Spring Boot do projeto principal na porta 8080.
4. Abra `br.com.stowup.staff.ui.Launcher` e clique em **Run**.

Por padrão a central usa `http://localhost:8080/api`. Para outro servidor, configure a variável `STOWUP_API_URL`.

O painel **Visão geral** apresenta usuários, proprietários verificados, espaços, reservas, contratos, chamados e receita estimada da plataforma, com atualização automática.

As aprovações, rejeições, suspensões, contratos e respostas são gravadas pela API no PostgreSQL compartilhado. Portanto, o que for alterado na central fica disponível no aplicativo assim que ele atualizar os dados.
