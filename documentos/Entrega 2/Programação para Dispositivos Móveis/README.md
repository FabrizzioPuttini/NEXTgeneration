# Programação para Dispositivos Móveis - Entrega 2

[APK debug instalável](proxima-etapa-entrega2.apk) · [Código Android](../../../src/Entrega2/Frontend) · [Roteiro de testes](../../../src/Entrega2/TESTAR.md)

Versão 2.0, versionCode 2, minSdk 24, assinatura debug de desenvolvimento.
API configurada: `https://nextgeneration-seven.vercel.app/`. Este APK não usa a fixture local.
A migração Supabase foi aplicada em 08/11/2026. Deploy E2 e teste integrado no ambiente
publicado ainda precisam de confirmação.

SHA256: `6a81b5c83260e1b73bbc373d2294c4582554ecf7762f6f63547c93dddb238475`.

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
