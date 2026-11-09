# Situação da Entrega 2 — 09/11/2026

## Estado publicado

Código integrado à `main`, com Entrega 1 preservada. API e painel publicados em
https://nextgeneration-seven.vercel.app/; `/health` e `/admin/` retornaram 200 em 09/11.
Luiz confirmou cadastro/login no app e acesso ao painel administrativo. O painel
publicado carregou cursos, encontros/presenças e mensagens na revisão em Chrome.
A migração `001_entrega2.sql` e o reparo de inscrições foram aplicados em 08/11,
conforme o [runbook](Backend/docs/migration-runbook.md).

Prazo informado pela equipe:09/11 às 23h56. Apresentação:19/11 às 19h (São Paulo).

## Correções autorizadas em 09/11

Luiz autorizou commit/push na main e ajuste no Supabase. A migração 002 foi aplicada
e conferida. Os commits foram preparados localmente; o push foi recusado por token
GitHub inválido. O deploy das correções permanece pendente de autenticação e envio.

- Atualização compatível de `body-parser` para 2.4.0 e `qs` para 6.17.1.
- Cadastro diferencia dados inválidos (400) de indisponibilidade/limitação temporária (503).
- Curso de encontro fixo após criação: API recusa mudança com 409; painel desabilita
  seletor ao editar e mantém escolha ao criar. Envie título/data/cancelamento para editar.
- Proteção complementar do vínculo no banco aplicada pela migração 002.
- Cabeçalhos de segurança configurados no Express e no `vercel.json` para CDN.
  A resposta hospedada anterior ainda não trazia os cabeçalhos da CDN; verificar após deploy.
- READMEs e contratos atualizados para distinguir estado publicado e alterações locais.

O APK não foi alterado nesta rodada. A versão da pasta de Dispositivos Móveis contém
a correção de datas e aponta para a API HTTPS publicada.

## Evidências e limites

| Verificação | Resultado |
|---|---|
| Android JVM |22 testes aprovados; reexecutados em 09/11|
| Android build e lint |Build aprovado; 0 erros e 102 avisos de lint|
| Layouts Entrega2 |Nenhum LinearLayout próprio; ConstraintLayout e containers especializados|
| APK versionado |Idêntico ao build conferido; URL HTTPS e hash no README da disciplina|
| Backend e painel |34/34 HTTP/SQL e 9/9 núcleo do painel; [relatório das correções](Backend/docs/revisao-2026-11-09.md)|
| Dependências de produção |npm audit: 0 vulnerabilidades em 09/11 após atualização local|
| API pública |health 200; rotas privadas sem token 401|
| Painel local corrigido |Chrome confirmou curso bloqueado na edição e habilitado na criação|
| Arquivos acadêmicos |APK, UML de 1 página, banner, análise/CSV/boxplot, extensão e declaração IA presentes|
| Links Markdown locais |Nenhum link quebrado detectado|

A revisão estática Android conferiu datas, sessão, cache separado por usuário, QR,
sincronização e certificados. Não substitui teste funcional atual no aparelho.

Testes anteriores confirmados por Luiz no Redmi com backend local: QR online,
fila offline/sincronização sem duplicação, câmera vertical, tema claro/escuro,
abertura e salvamento do certificado demonstrativo. Instrumentação anterior foi
executada no emulador; não foi repetida nesta rodada para preservar a sessão do usuário.
A busca limitada de padrões de tokens/chaves não encontrou ocorrências nos arquivos
examinados; não equivale a auditoria integral de segredos ou do histórico Git.

## Materiais acadêmicos

- [Extensão preenchida pela equipe](<../../documentos/Documento - Projeto de Extensão - COM Empresa - 2026_1 - Proxima Etapa.docx>) e
  [declaração IA](<../../documentos/Ficha de Declaracao Uso IA - FECAP.docx>) estão em documentos.
  O modelo original continua separado; fatos, campos e assinaturas devem ser conferidos pela equipe.
- Banner final incluído na pasta de Projeto Interdisciplinar.
- UML escolhido pela equipe, reunido em uma página; Luiz informou aprovação da professora.
  O guia descreve a relação entre modelo de domínio e implementação.
- Documento de análise, base simulada, resumo e boxplots presentes. Não houve nova
  auditoria integral dos cálculos nesta rodada de correção do software.
- [Contas e roteiro de avaliação](<../../documentos/Entrega 2/Projeto Interdisciplinar - Aplicativo Móvel/ACESSO_AVALIACAO.md>).
  Senhas entregues separadamente aos professores, fora do repositório público.

## Para fechar a validação integrada

1. Restabelecer autenticação GitHub, enviar os commits autorizados e confirmar deploy.
2. Após deploy, conferir headers e repetir login do avaliador, inscrição, QR online,
   repetição sem duplicidade, offline/sincronização dentro da validade e histórico nos dois clientes.
3. Conferir visualmente datas no APK final; testar fonte ampliada e recusa de permissões.
4. Preparar encontro para 19/11 às 19h e inscrição do aluno de demonstração. Os encontros
   vistos no painel em 09/11 eram de outubro. Gere QR próximo ao teste porque expira.
5. Conferir certificado real autorizado, se usado na apresentação; o PDF demonstrativo
   embarcado não representa emissão real nem conclusão de curso.

## Limites do produto

Notificações são consulta periódica, não push instantâneo. QR offline pode expirar
antes do envio; app não confirma presença sem servidor. Certificados são PDFs HTTPS
previamente emitidos, não emissão automática por frequência. Teste de interesses é
orientativo, sem validade psicométrica. Não há paginação além dos limites do contrato.
Concorrência SQL multiconexão não foi demonstrada pelos testes PGlite locais.
