const supabaseConfig = require('../config/supabase');

// Lista todos os cursos
exports.listarCursos = async (req, res) => {
    try {
        const client = req.supabase || supabaseConfig.supabase;

        if (!client || typeof client.from !== 'function') {
            console.error("Supabase client is invalid:", client);
            return res.status(500).json({ error: 'Erro de configuração do banco de dados.' });
        }

        // Busca todos os registros da tabela 'courses' no Supabase
        const { data, error } = await client
            .from('courses')
            .select('*');

        if (error) throw error;

        return res.status(200).json(data);
    } catch (error) {
        console.error("Erro ao listar cursos:", error);
        return res.status(500).json({ error: 'Erro ao buscar a lista de cursos.' });
    }
};

// Busca um curso específico pelo ID
exports.buscarCursoPorId = async (req, res) => {
    try {
        const { id } = req.params;
        const client = req.supabase || supabaseConfig.supabase;

        const { data, error } = await client
            .from('courses')
            .select('*')
            .eq('id', id)
            .single();

        if (error) throw error;

        if (!data) {
            return res.status(404).json({ message: 'Curso não encontrado.' });
        }

        return res.status(200).json(data);
    } catch (error) {
        console.error("Erro ao buscar curso:", error);
        return res.status(500).json({ error: 'Erro ao buscar os detalhes do curso.' });
    }
};

// Inscreve o aluno logado em um curso
exports.inscreverCurso = async (req, res) => {
    try {
        const userId = req.userId;
        const cursoId = req.params.id;
        const client = req.supabase || supabaseConfig.supabase;

        const { error } = await client
            .from('enrollments')
            .insert({
                user_id: userId,
                course_id: cursoId,
                status: 'enrolled'
            });

        if (error) throw error;

        return res.status(200).json({ message: 'Inscrição realizada com sucesso.' });
    } catch (error) {
        console.error("Erro ao realizar inscrição:", error);
        return res.status(500).json({ error: 'Erro ao realizar inscrição no curso.' });
    }
};
