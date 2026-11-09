# Aplicação da migração E2

Aplicada no Supabase em08/11/2026, após confirmação explícita do Luiz, pelo SQL Editor
do Chrome pessoal. `000a_repair_enrollments.sql` terminou com7 cópias arquivadas e0 grupos
duplicados; `001_entrega2.sql` retornou sucesso. Verificação remota:6 inscrições ativas,
7 tabelas E2 com RLS,9 funções E2,3 perguntas; escrita direta em inscrições/presenças/roles
bloqueada para anon/authenticated, arquivo e QR privados, execução anônima das RPCs negada.
Os13 registros originais (ativos+arquivo) foram comparados campo a campo com o backup local:
igualdade integral. Backup e screenshots ficam em `.codex/state/supabase-preflight/`,
ignorados pelo Git; nenhum dado pessoal foi incluído neste documento.

Publicação E2, provisionamento administrativo e testes Auth/PostgREST continuam pendentes.
Luiz aceitou a interrupção das inscrições E1 até a publicação, mantendo validação/merge
da branch sob sua responsabilidade. Não executar restore pré-E2 após esta ativação.

`001_entrega2.sql` foi testada em PostgreSQL embedded PGlite
com fixture sintética do schema confirmado, não em uma cópia integral do projeto remoto.
PGlite usa uma conexão exclusiva: não prova corrida multiconexão nem configuração Supabase hospedada.

1. Confirmar ambiente/projeto correto e responsabilidade pelo apply. Salvar backup/export do schema,
   grants/policies e dados afetados por meio autorizado antes da janela de alteração.
2. Rodar `000_preflight.sql` (leitura) e comparar campos/defaults com o schema esperado.
   Conferir roles/grants herdados. O preflight de08/11 identificou quatro grupos de inscrições
   repetidas, 11linhas/sete cópias excedentes. Todas estão `enrolled`, com zero pontos e sem
   presença registrada. Não há FK inbound nem trigger próprio em enrollments.
   Aplicar o reparo limitado descrito abaixo somente após revisar o backup e autorizar o movimento.
3. Confirmar que as tabelas e2_* não pertencem a outra aplicação. Conferir versão PostgreSQL,
   constraints, defaults adicionais obrigatórios e impacto de locks/índices em tabelas existentes.
4. Revisar e aprovar `001_entrega2.sql`, incluindo mudança de permissões e janela até publicar E2.
   Executar como owner controlado no SQL Editor.
   Há transação, lock_timeout5s e statement_timeout30s: erro deve causar rollback total.
5. Conferir que alunos não conseguem gravar attendances/enrollments/user_roles via REST direto,
   nem listar códigos QR ou resultados de outros usuários. Validar role administrativa existente
   em user_roles; provisionamento de admin é separado e exige autorização.
6. Publicar backend e Android E2 juntos. Testar contas fictícias de aluno/admin, presença repetida,
   QR inválido e de outra atividade, agenda/histórico, perfil, quiz, suporte e certificados.

## Mudanças e compatibilidade

Adiciona e2_* e activities.is_cancelled, índices únicos em presença/inscrição, funções e políticas
com prefixo e2. Não apaga dados. Restrição única pode falhar se o banco tiver duplicidades: para,
não deduplica automaticamente. Novos índices podem bloquear por curta janela limitada por timeout.

Revoga INSERT/UPDATE/DELETE (incluindo grants por coluna) de attendances, enrollments e user_roles
para PUBLIC/anon/authenticated. Presença e inscrição passam somente pelas RPCs autenticadas.
Leitura E1 preservada; **backend E1 deixa de poder inserir inscrições diretamente após a migração**.
GET cursos E2 passa a exigir JWT: o APK E1 sem token nessa chamada não é compatível.

## Rollback

Antes de COMMIT, qualquer erro desfaz a migração inteira. Depois do apply, não executar DROP de
tabelas nem desfazer dados. Preferir corrigir/restaurar o deployment E2 conhecido e manter dados.
Se restaurar backend E1, comunicar indisponibilidade de inscrição até adaptação da rota para RPC.
Reabrir grants antigos reintroduz bypass de QR/vagas/roles e exige revisão e aprovação específica.
Backup/restore remoto não foi testado nem autorizado nesta sessão.

## Reparo das sete cópias de inscrição

`000a_repair_enrollments.sql` é uma etapa separada; não rodar todos os arquivos `000*`
em sequência, pois `000b_restore_enrollments.sql` é exclusivamente recuperação.

O reparo exige exatamente os quatro grupos/sete excedentes observados, preserva a inscrição
mais antiga de cada grupo (UUID desempata datas iguais) e move os sete excedentes para
`e2_recovery.enrollment_duplicates_20261108`, com todos os campos originais e fotografia da
inscrição canônica. Conta e compara as cópias antes de retirá-las da tabela ativa; cria o índice
único na mesma transação. A operação não atribui presença, pontos, nota ou conclusão de curso.

O schema de recuperação tem RLS, sem grants para PUBLIC/anon/authenticated. Não o adicionar aos
schemas expostos do Supabase. Contém identificadores dos alunos: não exportar para Git, logs ou
documentação. **Esse arquivo está no mesmo banco e não substitui backup independente.**

`SHARE ROW EXCLUSIVE` bloqueia escritores concorrentes antes de contar/copiar, permite consultas
e termina no COMMIT. Limites de5s para lock e30s para statement interrompem uma janela longa.
Os testes PGlite verificam a lógica transacional, mas não demonstram concorrência multiconexão
ou duração de locks no servidor hospedado.

Após o reparo, conferir `archived_copies=7`, `duplicate_groups=0`, e conservação de contagem:
linhas ativas + sete arquivadas = quantidade anterior. Nenhum certificado ou presença é removido.
Um rerun sem duplicatas não move novas linhas. Se dados/grupos/schema/dependências divergirem,
o script para com `REPAIR_*`; não remover os guardrails para forçar execução.

O reparo preserva grants E1 e permite inscrição em curso novo. O índice passa a impedir nova
duplicata. A rota E1 traduz esse conflito em500; E2 usa RPC idempotente e retorna sucesso para
inscrição existente. Aplicar001 revoga INSERT E1: coordenar a indisponibilidade temporária até
publicar E2. Não devolver INSERT aberto como solução permanente, pois contorna controle de vagas.

### Recuperação limitada antes de001

`000b_restore_enrollments.sql`, após aprovação específica, recompõe os sete registros a partir
do arquivo e confere igualdade completa. Recusa se a canônica mudou, se um ID foi reutilizado,
se a contagem arquivada divergiu ou se `e2_enroll(uuid)` já existe. O arquivo não é apagado.
Remover o índice para restaurar as cópias reabre duplicações; isso não é rollback seguro após
ativar E2. Para ambiente E2 ativo, preferir correção progressiva e análise do responsável.

Validação local: `npm test` em `src/Entrega2/Backend`, incluindo conservação exata, recuperação,
negativos de acesso, limite de escopo, schema/FK divergente, compatibilidade E1 de inscrição
nova e aplicação001 após reparo. Nenhum teste local usa backup ou identificador real.

## Regras acadêmicas explícitas

Quiz é orientação exploratória de três perguntas, sem diagnóstico; empate usa ordem alfabética.
Certificado é URL de documento previamente emitido; administrador deve confirmar que os critérios
da organização foram atendidos. O sistema não inventa certificados nem determina regra oficial
de frequência. Novo cadastro sem profile ainda exibe nome da identidade/Aluno e escola vazia,
sem alegar que a ONG já cadastrou informações institucionais do participante.
