const supabase = require('../config/supabase');

const getPerfilAluno = async (req, res) => {
  try {
    // O ID do usuário logado deve vir do token JWT/sessão atual (ex: via middleware de auth)
    const userId = req.userId; 

    // Consulta à tabela profiles buscando todos os campos úteis do novo banco
    const { data, error } = await req.supabase
      .from('profiles')
      .select('full_name, school')
      .eq('id', userId)
      .single();

    if (error) {
      console.error('Erro ao buscar perfil:', error);
      return res.status(400).json({ erro: 'Não foi possível carregar os dados do perfil.' });
    }

    if (!data) {
      return res.status(404).json({ erro: 'Perfil não encontrado.' });
    }

    // Retorna os dados formatados para o frontend (Android)
    return res.status(200).json({
      nome: data.full_name,
      escola: data.school
    });

  } catch (err) {
    console.error('Erro interno no servidor:', err);
    return res.status(500).json({ erro: 'Erro interno no servidor.' });
  }
};

module.exports = {
  getPerfilAluno
};