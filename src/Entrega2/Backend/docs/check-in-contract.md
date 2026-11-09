# Presença — contrato transacional v1

Implementação atual: `POST /api/presencas/check-in {qrToken,activityId}` chama
`public.e2_check_in(p_token,p_activity_id)` usando o JWT verificado. O serviço puro
da primeira fatia foi substituído e removido.

QR contém JSON `{v:1,activityId:"UUID",qrToken:"token opaco"}`. A emissão administrativa
usa 32 bytes aleatórios seguros; somente SHA-256 do token é persistido. A identidade
vem de auth.uid(), nunca do corpo. A atividade declarada é conferida contra o token.

A RPC valida inscrição, encontro não cancelado e janela no relógio do banco,
obtém locks e insere sob índice único `(user_id,activity_id)`. Reenvio válido
retorna a mesma presença e created=false. A janela inclui opens_at e exclui expires_at.
Após expiração retorna410 mesmo com presença existente: cliente deve consultar
histórico por activity_id para reconciliar resposta perdida. Não há aceitação de
horário autodeclarado nem garantia de check-in offline fora da janela.

INSERT/UPDATE/DELETE diretos de presenças e inscrições são revogados dos clientes;
RLS restringe leitura própria, ampliada a administradores autenticados.
Falhas operacionais viram503 sem detalhes internos. Códigos e respostas estão no
[contrato HTTP](http-contract.md). A migração foi aplicada em 08/11/2026, conforme o runbook.
O ensaio completo de QR online/offline hospedado ainda deve ser repetido: testes
locais não equivalem a integração remota nem prova de concorrência multiconexão.
