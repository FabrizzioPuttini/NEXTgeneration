# Explicando o código da Entrega 2

Este guia apoia o estudo e a demonstração do código existente. A equipe deve abrir os arquivos e conseguir explicar o comportamento, inclusive limites. Não afirma que todos os testes de dispositivo ou de produção já foram executados.

## Caminho de uma presença

1. `HubActivity` recebe o conteúdo lido pela biblioteca de QR e chama `QrPayload.parse`. A validação local confere formato, UUID e token; ela não confirma presença.
2. `AlunoRepository.enqueue` persiste o check-in no SQLite, associado ao `user_id`. O par `(user_id, activity_id)` é chave primária, impedindo duas pendências para o mesmo encontro/usuário. Uma tentativa antes recusada pode ser atualizada com novo QR.
3. `SyncScheduler` agenda `SyncWorker`. O worker confere a conta dona da fila, usa `ApiClient` e envia ao backend.
4. `ApiClient` usa a interface Retrofit `ApiService`; `SessionManager` mantém os tokens criptografados com Android Keystore e apoia a renovação da sessão.
5. A API autentica o JWT. `e2_check_in` valida atividade, inscrição e validade do código e registra a presença no banco. O índice único `(user_id, activity_id)` evita duplicidade no servidor.
6. O worker marca a pendência como confirmada apenas após resposta/reconciliação. Se perdeu a resposta e uma tentativa posterior for recusada, consulta o histórico antes de concluir que falhou.
7. Ao atingir estado terminal, `AlunoRepository.state` apaga o token QR local. Falha de rede mantém a pendência para tentativa posterior.

O celular não decide sozinho se o estudante esteve presente: o QR prova acesso ao código emitido e o servidor aplica as regras. A leitura pela câmera real e a sincronização offline foram confirmadas por Luiz com backend local; a integração publicada ainda precisa de demonstração própria.

## Princípios de POO presentes

| Princípio | Exemplo verificável | Explicação |
|---|---|---|
| Encapsulamento | `AlunoRepository` tem `helper` privado | A tela usa métodos de cache/fila, sem manipular diretamente o banco. |
| Abstração | `ApiService` é uma interface | Declara operações HTTP que Retrofit implementa; `ApiClient` concentra chamadas e erros. |
| Composição | `AlunoRepository` cria um `AlunoDbHelper` | O repositório contém o objeto que administra o SQLite e o fecha em `close()`. |
| Herança | `SyncWorker extends Worker`; `AlunoDbHelper extends SQLiteOpenHelper` | Classes do projeto especializam os pontos de extensão do Android/AndroidX. |
| Polimorfismo | `doWork()`, `onCreate()` e métodos do adaptador com `@Override` | A biblioteca chama a implementação específica por meio do contrato da classe base. |
| Imutabilidade | `QrPayload` tem campos `final` e construtor privado | Uma instância validada não troca de atividade/token depois de criada. |
| Gerenciamento de recurso | `AlunoRepository implements AutoCloseable` | `try` com recursos fecha banco/cursores mesmo em falha. |

`ResourceListFragment` controla a view `RecyclerView`, sua configuração e limpeza em `onDestroyView`. O XML usa `FragmentContainerView`. `HubActivity` implementa a interface interna `AdapterHost` e fornece o adaptador `Rows` por `resourceAdapter()`: o fragmento administra a view, enquanto a Activity mantém os dados e ações. Isso permite exemplificar interface e lifecycle com código real.

`AuthAttemptGate` usa uma geração de tentativa para descartar callbacks antigos de login/cadastro. `AuthInputPolicy` concentra a regra mínima de oito caracteres para cadastro, sem remover espaços da senha. `NextMeetingPolicy.next` percorre a agenda e seleciona o horário futuro mais próximo em `America/Sao_Paulo`; assim um encontro antigo não aparece como próximo apenas por ocupar a primeira posição. O percurso é linear no número de encontros, sem ordenar toda a coleção.

`HubActivity` concentra várias telas/estados: é uma simplificação do projeto, não uma afirmação de arquitetura ideal. Uma evolução possível é separar controladores de cada recurso. A API trata entidades como JSON; não se deve apresentar classes de domínio Java que não existem.

## Decisões de estruturas de dados

**`HashMap<String, String>` para respostas do questionário.** Em `HubActivity`, a chave é o ID da pergunta e o valor é a alternativa escolhida. `put` substitui a escolha anterior da mesma pergunta, mantendo uma resposta por chave. Consulta e atualização têm custo médio constante; não há garantia de ordem de iteração. A API valida o conjunto independentemente da ordem. Compare com uma lista de pares que exigiria procurar a pergunta para substituir a escolha.

**`HashSet<String>` para IDs de presenças já confirmadas.** Na montagem do histórico, `HubActivity` reúne IDs recebidos do servidor. Ao combinar com pendências locais, `contains` evita apresentar duplicidades. Construir o conjunto e percorrer pendências tem custo médio proporcional a `n + m`, em vez de comparar todas as combinações (`n × m`). Não significa que toda a tela seja constante: renderização e parsing continuam percorrendo os dados.

**`ArrayList` para listas de apresentação.** `Rows` utiliza `List<Row>` com implementação `ArrayList`. O adaptador acessa posições (`get(p)`) e conhece `size()`, adequados à `RecyclerView`. `AlunoRepository.checkins` também monta `List<Pending>` a partir do cursor. Objetos `Row`, `Holder`, `Rows`, `Cached`, `Pending` e `Session` são classes internas reais.

**Fila persistida no SQLite.** Pendências são linhas duráveis, não uma `Queue` Java em memória. Na implementação atual, `checkins` ordena por `created_at DESC`, portanto processa as mais recentes primeiro: **não apresentar como FIFO**. A persistência permite sobreviver ao fechamento do processo; a chave composta isola contas e elimina duplicatas. O estado é limitado a `pending`, `confirmed` e `rejected` pelo esquema. Alterações de versão do banco exigirão migração explícita; o código não descarta a fila automaticamente.

**Map/Set também no servidor.** A listagem de cursos monta um `Map` por `course_id` para associar a situação da inscrição. Notificações usam `Set` de IDs lidos. São coleções JavaScript, não classes Java. Consultas usam filtros por usuário e limites; o app não carrega histórico ilimitado.

## Roteiro curto de apresentação

- Abra o PDF escolhido pela equipe e apresente as entidades do domínio. Depois mostre no código HubActivity, AlunoRepository, SyncWorker e ApiClient no fluxo de presença; consulte no README o mapeamento entre domínio e implementação.
- Mostre que IDs são UUID e nomes não servem como chave.
- Abra `AlunoRepository.java`: mostre `enqueue`, chave composta no helper e consulta filtrada pelo dono.
- Abra `HubActivity.java`: localize `answers`, `historyIds` e `Rows`; explique o motivo de Map, Set e List.
- Abra `SyncWorker.java`: mostre diferença entre pendência, confirmação e recusa, incluindo reconciliação.
- Execute a demonstração com dados fictícios: QR válido, repetido e perda de rede. Explique os resultados observados sem alegar testes que não foram feitos.

## Perguntas que a equipe precisa saber responder

- Por que um mesmo QR pode gerar presenças de alunos diferentes, mas não duas para o mesmo aluno/encontro?
- Por que uma leitura offline fica pendente, e pode ser recusada quando o código expira?
- Como o aplicativo evita que a conta B veja a fila da conta A?
- Qual a diferença entre uma classe Java, uma entidade persistida e um objeto JSON retornado pela API?
- Onde há herança, composição, interface, Map, Set e List no código?
- Por que a ausência de internet não deve resultar em confirmação fictícia?

As respostas estão nos métodos indicados, no contrato HTTP e nos testes. Este guia não substitui o ensaio de cada integrante.

## Diagrama da entrega e código

O PDF escolhido pela equipe é o modelo de domínio enviado originalmente, cuja aprovação pela professora foi informada por Luiz. Ele não descreve literalmente todas as classes Java nem substitui este guia técnico. Para explicar a implementação, use os exemplos verificáveis acima e o mapeamento do README.
