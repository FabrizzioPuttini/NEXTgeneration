const supabaseConfig = require('../config/supabase');

async function cadastrar(req, res) {
  try {
    const { nome, email, senha } = req.body;

    if (!nome || !email || !senha) {
      return res.status(400).json({
        error: 'Nome, email e senha são obrigatórios.'
      });
    }

    const { data, error } = await supabaseConfig.supabase.auth.signUp({
      email,
      password: senha,
      options: {
        data: {
          full_name: nome
        }
      }
    });

    if (error) {
      return res.status(400).json({
        error: error.message
      });
    }

    return res.status(201).json({
      message: 'Cadastro realizado com sucesso.',
      user: data.user
    });

  } catch (error) {
    return res.status(500).json({
      error: 'Erro interno do servidor.'
    });
  }
}

async function login(req, res) {
  try {
    const { email, senha } = req.body;

    if (!email || !senha) {
      return res.status(400).json({
        error: 'Email e senha são obrigatórios.'
      });
    }

    const { data, error } = await supabaseConfig.supabase.auth.signInWithPassword({
      email,
      password: senha
    });

    if (error) {
      return res.status(401).json({
        error: error.message
      });
    }

    return res.status(200).json({
      message: 'Login realizado com sucesso.',
      access_token: data.session.access_token,
      refresh_token: data.session.refresh_token,
      user: data.user
    });

  } catch (error) {
    return res.status(500).json({
      error: 'Erro interno do servidor.'
    });
  }
}

module.exports = {
  cadastrar,
  login
};