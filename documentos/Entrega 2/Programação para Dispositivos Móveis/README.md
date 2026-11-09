# Programação para Dispositivos Móveis - Entrega 2

[APK debug instalável](proxima-etapa-entrega2.apk) · [Código Android](../../../src/Entrega2/Frontend) · [Roteiro de testes](../../../src/Entrega2/TESTAR.md)

Versão 2.0, versionCode 2, minSdk 24, assinatura debug de desenvolvimento.
API configurada: `https://nextgeneration-seven.vercel.app/`. Este APK não usa a fixture local.
A migração Supabase foi aplicada em 08/11/2026. Em 09/11/2026, o painel respondeu HTTP 200 e a API `/health` retornou `status: ok`. O teste completo de QR no ambiente publicado ainda está pendente.

[Contas de avaliação, painel e instruções de acesso](ACESSO_AVALIACAO.md).

SHA256: `e0ceb4077091cf65db8f76621119d8d2f107511ba666277dee8d9c0023611062`.

## Verificação

Em 08/11/2026: build aprovado, 21/21 testes JVM, lint com 0 erros e 102 avisos.
O APK desta pasta é idêntico ao artefato gerado na verificação. Há também registro anterior
de 6/6 testes instrumentados no emulador; eles não foram repetidos nesta revisão documental.

No Redmi Note 8, usando APK de teste com backend local, Luiz confirmou:
- leitura de QR e presença online;
- leitura sem conexão, pendência e sincronização posterior sem duplicação;
- câmera do leitor na vertical após atualização do APK;
- abertura e download do PDF de demonstração.

O teste usou um certificado fictício ("Dummy PDF file"), não um certificado final da ONG.
Continuam sem comprovação completa: fluxo hospedado sem cabo, fonte ampliada e cenários
de permissão negada no celular. [Situação e limites](../../../src/Entrega2/STATUS.md).

## PDF de demonstração embarcado

O APK inclui um certificado fictício próprio do projeto. Em **Perfil → Certificados →
Certificado de demonstração**, é possível abrir o PDF em um leitor e salvá-lo pelo
seletor de arquivos do Android. O documento funciona offline, após acesso ao app,
e permanece separado dos certificados reais listados pela API.

Verificação do recurso demo: 4/4 testes instrumentados específicos no emulador,
com API indisponível, incluindo recriação da tela, URI de leitura, cópia do PDF e
isolamento do diretório compartilhado. Build/JVM/lint aprovados. A escolha de pasta
no seletor e a abertura em leitor externo precisam ser confirmadas no celular.

## Conferência do APK em 09/11/2026

APK recompilado a partir do código Android atual, com `API_BASE_URL=https://nextgeneration-seven.vercel.app/`. Build `assembleDebug` aprovado e assinatura verificada. O arquivo versionado foi substituído por esse artefato; a versão permanece 2.0 (versionCode 2). Esta conferência não repete os testes funcionais no aparelho.

## Datas nas telas

Em 09/11/2026, as datas de mensagens, comunicados, notificações, resultados do perfil, presenças, certificados e resumo do próximo encontro foram ligadas ao formatador já utilizado em cursos e agenda. Instantes da API são exibidos em horário de São Paulo (ex.: `09/11/2026 · 16h27`), sem expor o timestamp ISO. Datas/horários separados da agenda são preservados como horário local do encontro.

Verificação desta correção: 22 testes JVM aprovados, build aprovado e lint com 0 erros e 102 avisos. Teste de regressão cobre os dois timestamps enviados na captura da conversa. A revisão foi feita no código e nos testes; a conferência visual do novo APK no aparelho ainda deve ser realizada.
