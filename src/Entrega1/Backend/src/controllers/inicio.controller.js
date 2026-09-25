const getDadosInicio = async (req, res) => {
    try {
        // ID do usuário que foi validado pelo middleware de autenticação.
        const userId = req.userId;

        // Client do Supabase autenticado com o token do usuário.
        const supabase = req.supabase;

        // Busca o nome do usuário na tabela profiles.
        // maybeSingle() evita estourar erro 500 caso o perfil ainda não tenha sido criado pelo banco
        const { data: perfil, error: perfilError } = await supabase
            .from('profiles')
            .select('full_name')
            .eq('id', userId)
            .maybeSingle();

        if (perfilError) {
            console.error('Erro ao buscar perfil:', perfilError);
            return res.status(400).json({
                erro: 'Não foi possível carregar os dados do usuário.'
            });
        }

        const nomeFinal = perfil ? perfil.full_name : 'Aluno';

        // Busca a inscrição do usuário em um curso.
        // Como o aluno pode ter se inscrito em MAIS DE UM curso na tela de Cursos,
        // usar maybeSingle() pode dar erro se o banco retornar 2 ou mais inscrições.
        // Usamos .limit(1) para garantir que só vamos pegar o primeiro curso ativo.
        const { data: inscricoes, error: inscricaoError } = await supabase
            .from('enrollments')
            .select('course_id')
            .eq('user_id', userId)
            .eq('status', 'enrolled')
            .limit(1);

        if (inscricaoError) {
            console.error('Erro ao buscar inscrição:', inscricaoError);
            return res.status(400).json({
                erro: 'Não foi possível verificar as inscrições do usuário.'
            });
        }

        let nomeCurso = null;

        if (inscricoes && inscricoes.length > 0) {
            const inscricao = inscricoes[0];
            // Busca o curso usando o course_id encontrado na inscrição.
            const { data: curso, error: cursoError } = await supabase
                .from('courses')
                .select('title')
                .eq('id', inscricao.course_id)
                .single();

            if (cursoError) {
                console.error('Erro ao buscar curso:', cursoError);
                return res.status(400).json({
                    erro: 'Não foi possível carregar o curso do usuário.'
                });
            }

            nomeCurso = curso.title;
        }

        // Retorna somente os dados que a Página Inicial precisa.
        // curso vem null quando o usuário não está inscrito em nenhum curso.
        return res.status(200).json({
            nome: nomeFinal,
            curso: nomeCurso
        });

    } catch (error) {
        console.error('Erro interno ao carregar Página Inicial:', error);

        return res.status(500).json({
            erro: 'Erro interno no servidor.'
        });
    }
};

module.exports = {
    getDadosInicio
};