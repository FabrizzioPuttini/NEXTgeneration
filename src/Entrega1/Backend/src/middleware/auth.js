// Middleware de autenticação.
//
// Lê o JWT enviado pelo app Android no header Authorization, cria um
// client Supabase autenticado com esse token e confirma que o usuário
// é válido chamando supabase.auth.getUser(). Se tudo estiver certo,
// pendura o client autenticado (req.supabase) e o id do usuário
// (req.userId) na requisição, para os controllers usarem.
const { createSupabaseClientForUser } = require('../config/supabase');

async function auth(req, res, next) {
  const authHeader = req.headers.authorization;

  // Sem header ou sem o prefixo "Bearer " -> não tem token.
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Token ausente' });
  }

  const token = authHeader.slice('Bearer '.length).trim();

  if (!token) {
    return res.status(401).json({ error: 'Token ausente' });
  }

  try {
    // Cria o client repassando o JWT do usuário — a partir daqui, toda
    // consulta feita com esse client já respeita a RLS configurada no banco.
    const supabase = createSupabaseClientForUser(token);

    // Valida o token junto ao Supabase Auth e obtém os dados do usuário.
    const { data, error } = await supabase.auth.getUser(token);

    if (error || !data || !data.user) {
      return res.status(401).json({ error: 'Token inválido' });
    }

    req.supabase = supabase;
    req.userId = data.user.id;

    next();
  } catch (error) {
    // Qualquer falha inesperada na validação do token também é tratada
    // como token inválido, sem deixar o processo cair.
    res.status(401).json({ error: 'Token inválido' });
  }
}

module.exports = auth;
