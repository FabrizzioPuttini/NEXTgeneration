// Factory de client Supabase.
//
// IMPORTANTE: nunca criamos um client "global" com a service_role key aqui.
// Cada requisição autenticada cria o seu próprio client, repassando o JWT
// do aluno logado. Assim o Postgres reconhece esse aluno via auth.uid()
// e a Row Level Security (RLS) filtra os dados automaticamente — o
// backend não precisa (e não deve) escrever nenhum "WHERE student_id = ...".
const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = process.env.SUPABASE_URL;

const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {

  // Falha rápido: sem essas variáveis o servidor não tem como funcionar.

  throw new Error(

    'SUPABASE_URL e SUPABASE_ANON_KEY precisam estar definidos no .env'

  );

}

// Client usado para cadastro e login.
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

/**

 * Cria um client Supabase autenticado com o token do usuário logado.

 * Esse client deve ser usado em todas as consultas feitas dentro da

 * requisição, para que a RLS seja aplicada corretamente.

 *

 * @param {string} accessToken - JWT recebido no header Authorization

 * @returns client Supabase configurado com o token do usuário

 */

function createSupabaseClientForUser(accessToken) {

  return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {

    global: {

      headers: {

        Authorization: `Bearer ${accessToken}`,

      },

    },

  });

}

module.exports = {supabase,createSupabaseClientForUser};