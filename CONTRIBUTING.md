# Como colaborar

1. Clone o repositório e execute `INSTALAR.bat` uma vez.
2. Crie uma branch para cada alteração.
3. Antes de enviar, rode os testes descritos no `README.md`.
4. Abra um pull request explicando o que mudou e como foi testado.

Não envie `.runtime/`, banco local, uploads, documentos de usuários, arquivos `.env`, senhas ou tokens. Esses itens já estão cobertos pelo `.gitignore`, mas cada colaborador continua responsável por revisar o próprio commit.

O código de produção deve preservar a separação entre domínio, casos de uso, portas e adaptadores. Integrações externas, como pagamento, e-mail e SMS, devem ficar atrás de portas para manter a arquitetura hexagonal.
