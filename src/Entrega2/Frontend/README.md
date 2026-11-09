# Próxima Etapa — Android, Entrega 2

Projeto Android Java/XML, minSdk 24, compileSdk/targetSdk 36, Java 11. Abra esta pasta no Android Studio. O código da Entrega 1 permanece em `../../Entrega1/Frontend` como registro histórico.

## Executar

1. Sincronize o Gradle no Android Studio e selecione um emulador ou Android conectado por USB.
2. O backend precisa estar com o contrato E2 publicado e seu SQL revisado aplicado. A URL de produção configurada é `https://nextgeneration-seven.vercel.app/`. O app não conecta diretamente ao banco Supabase.
3. Execute `app`. Crie uma conta de teste autorizada ou entre com uma existente.
4. Para o backend local, compile com `-PAPI_BASE_URL=http://localhost:3000/` e, com dispositivo conectado, configure `adb reverse tcp:3000 tcp:3000`. O debug permite HTTP somente para localhost, 127.0.0.1 e 10.0.2.2. Release exige HTTPS.

Comandos usados neste Mac (o Android Studio fornece o JDK):

```sh
export ANDROID_HOME=/Users/luizrs/Library/Android/sdk
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Emulador ou dispositivo de testes conectado:
bash gradlew :app:connectedDebugAndroidTest
# API local:
bash gradlew -PAPI_BASE_URL=http://localhost:3000/ :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. O build comum usa o backend HTTPS, enquanto o comando com propriedade gera APK de teste local; confira a configuração antes de entregar. Não há chaves Supabase no app.

## Fluxos para testar

- Login/cadastro, senha sem remover espaços, erro de rede, sair e entrar em outra conta.
- Cursos: lista real, detalhes, inscrição e mudança do status; agenda mostra encontros e presença.
- Agenda → Ler QR: conceder/recusar câmera, cancelar, QR alheio/inválido e QR válido emitido no painel.
- Leia o QR em modo avião: deve ficar **pendente**, persistir após fechar e abrir o app e sincronizar quando houver rede. QR expirado pode ser recusado pelo servidor; o aparelho nunca valida presença sozinho. Repetir o mesmo encontro não cria segunda presença.
- Perfil → Histórico: confirmadas, pendentes ou recusadas com motivo. Puxar a lista para baixo solicita atualização e sincronização. O histórico do servidor reconcilia uma resposta perdida.
- Perfil → Carteirinha, certificados HTTPS (abrir/baixar), questionário e resultados, dúvidas/respostas, comunicados e central de notificações.
- Avisos no celular pede permissão no Android 13+. São atualizações periódicas do WorkManager, com mínimo de 15 minutos e restrições do Android; não são push instantâneo.
- Teste telas com fonte ampliada e teclado aberto, principalmente autenticação, questionário e envio de dúvida.

## Como o código está organizado

- `AuthActivity`: login/cadastro; `MainActivity`: entrada; `HubActivity`: navegação e telas autenticadas; `ResourceListFragment`: componente real das listas, criado pelo FragmentContainerView, com descarte de view/adapter no lifecycle.
- `network/ApiClient`: Retrofit, erros HTTP e renovação serializada de sessão, sem loop de refresh.
- `network/SessionManager`: access/refresh token e user.id criptografados com chave Android Keystore.
- `data/AlunoDbHelper` e `AlunoRepository`: SQLite com cache e fila separados por user.id, queries parametrizadas e chave única usuário/encontro.
- `sync/SyncWorker`: envio, retry/backoff, reconciliação de histórico e avisos; `SyncScheduler`: trabalho imediato e periódico.
- `core/QrPayload` e `SyncPolicy`: validação do QR e política de retry, testadas na JVM.
- Telas e itens usam ConstraintLayout; as listas usam RecyclerView dentro de ResourceListFragment. Assets, cores e navegação preservam a identidade da E1. Layouts/DTOs obsoletos da E1 não foram copiados para a implementação final E2.

Logout cancela trabalhos e apaga sessão. Cache/fila permanecem no sandbox para o mesmo usuário retomar depois; outra conta nunca consulta sua partição. Banco e preferências estão excluídos de backup e transferência. Tokens de QR são apagados quando a fila chega a estado terminal.

## Evidência e limites

Consulte `docs/implementation.md`. Build e lint não substituem avaliação visual ou câmera real. As rotas reais dependem da implantação do backend e do provisionamento remoto autorizado; nenhum dado real foi alterado por esta implementação Android.

## Perfil, aparência e atualização

No topo de qualquer lista, puxe para baixo para atualizar. Leitores de tela também podem usar a ação “Atualizar conteúdo” no título.
Em **Perfil → Aparência → Escolher tema**, selecione Sistema, Claro ou Escuro. A escolha fica salva no aparelho, inclusive após fechar o app.
O perfil exibe nome, e-mail e escola retornados pela conta autenticada; idade não cadastrada aparece como não informada. O e-mail exige a versão atualizada do backend.

Teste os três temas, feche e reabra o aplicativo, atualize com e sem rede e confira que o indicador para ao finalizar.

## Organização visual

O Perfil separa a identidade (iniciais, nome e dados da conta) das opções de navegação. Toque em qualquer parte de uma linha para abrir a opção; o ícone e a seta são decorativos. O certificado demonstrativo continua identificado como exemplo sem validade.
O cabeçalho e a navegação usam ícones consistentes; cartões de cursos, agenda e demais conteúdos usam a mesma hierarquia de título, descrição e ação.

### Cursos e Agenda

Cursos têm cartão próprio com situação da inscrição, descrição e metadados separados. Agenda destaca dia/mês e informa horário, data completa, local e situação da presença. A leitura de QR permanece na ação principal.
Datas de cursos recebidas como instantes são exibidas no fuso America/Sao_Paulo; os campos locais da Agenda não sofrem outra conversão. Horários não mostram segundos e não exibem separador de término quando ele está ausente. Dados inválidos ou ausentes usam “a definir”.
