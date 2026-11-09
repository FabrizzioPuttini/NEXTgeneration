# Programação Orientada a Objetos e Estrutura de Dados - Entrega 2

[Diagrama da entrega em PDF](UML_Entrega2.pdf) · [Guia do código e estruturas de dados](GUIA_POO.md)

O desenho apresenta o modelo de domínio, com Usuario (Aluno), Curso, Atividades, Inscricao, Presenca, Certificado, Card, TestePerfil, Pergunta, Alternativa, ResultadoPerfil, Mensagem e Notificacao. A paginação foi ajustada para apresentar todo o desenho em uma página, sem redesenhar as classes ou relações.

## Relação com a implementação

O diagrama representa o modelo de domínio. Os nomes e métodos desenhados não são um inventário literal das classes Java: na implementação, parte dessas entidades é representada por tabelas e objetos JSON. O guia explica as classes e estruturas efetivamente usadas no código.

| Conceito | Representação implementada | Uso no aplicativo |
|---|---|---|
| Aluno | `profiles` e identidade autenticada; `/api/perfil` | `SessionManager` e `HubActivity` |
| Curso | `courses`; `/api/cursos` | `HubActivity.load/render` |
| Atividade | `activities`; `/api/agenda` | `HubActivity` e `NextMeetingPolicy` |
| Inscrição | `enrollments`; `/api/cursos/:id/inscricao` | `HubActivity.mutate` |
| Presença | `attendances`; `/api/presencas` e `/check-in` | `QrPayload`, `AlunoRepository.Pending` e `SyncWorker` |
| Certificado | `certificates`; `/api/certificados` | `HubActivity.certificate` |
| Card | Projeção de perfil; `/api/card` | `HubActivity.render` |
| TestePerfil | Objeto agregado de `/api/teste-perfil` | `HubActivity.choose/submitQuiz` |
| Pergunta | `e2_profile_questions` | JSON consumido por `HubActivity.choose` |
| Alternativa | JSON `alternatives` de cada pergunta | `HubActivity.answers` guarda a escolha |
| ResultadoPerfil | `e2_profile_results` | `HubActivity.submitQuiz/render` |
| Mensagem | `e2_messages`; `/api/mensagens` | `HubActivity.sendMessage` |
| Notificação | `e2_notifications` e `e2_notification_reads` | `SyncWorker` e `AlunoRepository` |

## Estruturas de dados

O guia apresenta Map para respostas do questionário, Set para evitar repetição visual de presenças e List para o RecyclerView. A fila offline é persistida em SQLite.

## Arquivo original

O PDF recebido é a fonte desta entrega. O arquivo draw.io do diagrama anterior foi retirado para não ser confundido com uma versão editável deste modelo; permanece recuperável no histórico Git. Não foi fornecido o arquivo editável correspondente ao PDF escolhido.
