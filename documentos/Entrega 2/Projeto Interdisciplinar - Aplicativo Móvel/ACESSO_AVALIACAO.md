# Acesso para avaliação — Entrega 2

## Aplicativo Android

Baixe o [APK da entrega](<../Programação para Dispositivos Móveis/proxima-etapa-entrega2.apk>) e instale em um Android 7.0 ou superior. No GitHub, use o botão de download do arquivo. Autorize a instalação por essa origem se o Android solicitar.

O aplicativo usa a API publicada e precisa de internet para entrar na conta. Não é necessário executar o backend local nem conectar o aparelho por USB.

| Acesso | Conta | Onde usar |
| --- | --- | --- |
| Aluno Demonstração | `aluno@example.test` | Aplicativo Android |
| Professor Avaliador (administrador) | `avaliador@example.test` | Painel da equipe |

**Senhas:** devem ser fornecidas pela equipe no canal restrito de entrega da disciplina. Não estão neste repositório público. Os endereços de avaliação são fictícios e não recebem e-mails de recuperação; procure a equipe se precisar redefinir o acesso.

## Painel administrativo

Abra https://nextgeneration-seven.vercel.app/admin/ e entre com a conta Professor Avaliador e a senha fornecida pela equipe. Essa conta tem permissão para gerenciar os dados do ambiente publicado. Use dados identificados como demonstração e preserve os registros existentes.

## Roteiro de avaliação

1. No APK, entre como Aluno Demonstração e confira o perfil, cursos e agenda.
2. Escolha um curso ativo com vaga e faça a inscrição.
3. No painel, entre como Professor Avaliador, localize o encontro desse curso e gere o QR no período de check-in.
4. No APK, abra Agenda → Ler QR de presença, autorize a câmera e leia o código exibido em outra tela.
5. Confira o histórico no APK e a presença no painel. A leitura repetida não deve duplicar a presença.
6. Para testar offline, use outro encontro disponível: leia seu QR sem conexão, volte à internet e sincronize a pendência dentro da validade do código.
7. Em Perfil, confira a troca de tema e o certificado de demonstração em PDF. Esse PDF é fictício, sem validade acadêmica, e é separado dos certificados emitidos pelo painel.
8. Opcionalmente, envie uma dúvida pelo aplicativo e responda pela aba Dúvidas do painel.

## Situação verificada em 09/11/2026

- Painel `/admin/`: HTTP 200; API `/health`: `{"status":"ok"}`.
- Luiz confirmou o acesso à sua conta administrativa no painel publicado.
- As contas Aluno Demonstração e Professor Avaliador existem e estão confirmadas no Supabase. A permissão administrativa do avaliador foi conferida no banco; seu login no painel ainda precisa ser testado.
- O fluxo completo de QR online/offline foi testado anteriormente com backend local. A repetição completa no ambiente publicado ainda está pendente.
- A apresentação está prevista para 19/11/2026 às 19h (São Paulo). O curso/encontro específico e a inscrição de demonstração ainda precisam ser preparados. Gere o QR perto do teste, pois ele expira.

Disponibilidade verificada nesta data não garante disponibilidade futura. Em caso de falha, informe à equipe o horário e a mensagem exibida, sem enviar senhas.
