# Como testar a Entrega 2

Comece pela demonstração local com dados fictícios. Depois repita o roteiro no backend
publicado. O APK padrão usa a URL HTTPS existente; confirme o deploy E2 antes do teste final.

## 1. Painel local, sem configurar Supabase

No terminal, dentro de `src/Entrega2/Backend`:

```sh
npm ci
node scripts/demo-server.js
```

Abra `http://127.0.0.1:38766/admin/` no Chrome. Login da equipe:
`admin@example.test`, senha `demonstracao123`. Esta senha serve apenas para o servidor
fictício local. Ele recusa execução em produção e reinicia os dados a cada abertura.
O aluno fictício já está inscrito no curso de demonstração.

## 2. Aplicativo no celular conectado por USB

1. Ative opções do desenvolvedor e depuração USB no celular; conecte o cabo e aceite
   a autorização exibida no aparelho.
2. Abra `src/Entrega2/Frontend` no Android Studio e espere a sincronização.
3. No terminal dessa pasta, execute os comandos abaixo. Eles geram e instalam uma
   versão de teste local, diferente do APK que aponta para a API publicada.

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME="$HOME/Library/Android/sdk"
"$ANDROID_HOME/platform-tools/adb" reverse tcp:38766 tcp:38766
bash gradlew -PAPI_BASE_URL=http://localhost:38766/ :app:installDebug
```

Se houver mais de um dispositivo conectado, selecione o aparelho no Android Studio
ou desligue o emulador de testes. Entre no aplicativo com `aluno@example.test` e
senha `demonstracao123`. Cadastro novo não funciona na fixture; é testado apenas
quando Supabase Auth está configurado.

Para emulador, também é possível usar `-PAPI_BASE_URL=http://10.0.2.2:38766/`.
O encaminhamento USB permite alcançar a API local sem expor portas públicas.
Mantenha também uma conexão de rede ativa no Android para o WorkManager liberar a
sincronização. Ao desconectar/reconectar o cabo, execute novamente `adb reverse`.
HTTP local só é permitido na versão debug.

## 3. Ambiente Supabase/Vercel

A migração no projeto Supabase atual foi aplicada em 08/11/2026; não a reaplique para
testar. Em um ambiente novo, siga o [runbook](Backend/docs/migration-runbook.md):
preflight, backup, migração autorizada e papel admin da conta definida pela equipe.
A migração altera permissões de inscrição/presença: voltar apenas ao backend E1
não restaura a compatibilidade de escrita.

O responsável pela Vercel configura Root Directory `src/Entrega2/Backend`, mantém
`SUPABASE_URL` e `SUPABASE_ANON_KEY` do projeto atual, publica e confere `/health`, login,
agenda e check-in. Não é preciso um segundo projeto para o painel `/admin/`.
Não usar service_role no app ou navegador.

Depois, gere o APK padrão sem `-PAPI_BASE_URL`:

```sh
bash gradlew :app:assembleDebug
```

Se tiver adicionado `API_BASE_URL` a `gradle.properties`, remova a configuração local
antes de gerar o APK de entrega. O arquivo resultante fica em
`app/build/outputs/apk/debug/app-debug.apk`. Instale e repita os cenários na API HTTPS.

## Cenário principal

- Administrador entra no painel, escolhe/cria curso e encontro e exibe um QR válido.
- Aluno entra no aplicativo, consulta o curso e faz inscrição.
- Agenda mostra o encontro correto, com local e horário.
- Aluno lê o QR no celular e vê presença confirmada após resposta da API.
- Histórico e painel mostram a mesma presença.
- Ler novamente não cria um segundo registro.

## Sem internet

- Abrir agenda conectado para salvar dados localmente.
- Ativar modo avião e reabrir a agenda: mostrar dados salvos e aviso offline.
- Ler QR durante sua validade: mostrar pendência, nunca confirmação antecipada.
- Restaurar a rede e sincronizar enquanto o QR ainda é válido.
- Se expirou, mostrar motivo; se a primeira tentativa já foi gravada, reconciliar com histórico.
- Fechar/reabrir o aplicativo não pode perder a fila.

## Conta e acesso

- Senha incorreta gera mensagem compreensível.
- Logout encerra acesso às telas privadas.
- Entrar como outro aluno não mostra nem envia a fila da conta anterior.
- Aluno comum não entra no painel administrativo.
- QR inválido, desconhecido ou de curso sem inscrição não confirma presença.

## Certificado de demonstração incluído no APK

Após entrar no app, abra **Perfil → Certificados → Certificado de demonstração**.
O arquivo é um PDF fictício incluído no aplicativo, independente da lista da API.

1. Escolha **Abrir PDF** e selecione um leitor de PDF, se o Android pedir.
2. Volte ao app, escolha **Salvar PDF** e indique uma pasta no seletor do Android.
3. Abra o arquivo salvo e confira o título "CERTIFICADO DE DEMONSTRAÇÃO".
4. Repita sem conexão: o documento está no APK e não exige acesso ao Vercel.

A opção é identificada como demonstração sem validade e não é um certificado obtido
pelo aluno. Não gera presença, conclusão nem registro no Supabase. Os certificados
reais continuam sendo listados pela API e abertos por URL HTTPS.

## Recursos adicionais

- Card mostra somente o aluno logado.
- Certificados listam documentos autorizados; abrir/baixar funciona e vazio é explicado.
- Teste exige respostas, salva resultado e informa caráter orientativo.
- Comunicado publicado aparece no aplicativo.
- Aluno envia dúvida; equipe responde; resposta aparece no mesmo canal.
- Notificações funcionam na lista interna mesmo se a permissão Android for negada.
- Notificações por consulta periódica não equivalem a push instantâneo.

## Evidências a guardar

Capturas sem dados reais: agenda, QR do encontro fictício, confirmação, histórico,
offline/pendência, painel, certificado e questionário. Registrar versão do APK e URL da API.
Uma tela vazia ou somente o health check não comprova integração completa.

