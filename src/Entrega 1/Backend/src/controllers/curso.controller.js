// src/controllers/curso.controller.js
const supabase = require('../config/supabase');

// Lista todos os cursos
exports.listarCursos = async (req, res) => {
    try {
        // Busca todos os registros da tabela 'cursos' no Supabase
        const { data, error } = await supabase
            .from('cursos')
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
        
        const { data, error } = await supabase
            .from('cursos')
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