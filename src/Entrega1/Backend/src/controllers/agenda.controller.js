 // Controller de agenda.
// Responsável por listar os encontros/aulas do aluno logado.
//
// O schema atual (schema.sql) não tem mais a view "my_agenda" usada
// antes. Também não habilita RLS nas tabelas envolvidas (enrollments,
// activities, attendances, courses) — ou seja, NÃO podemos confiar
// apenas no banco para restringir os dados ao aluno logado. Por isso,
// toda consulta aqui filtra explicitamente por req.userId.
//
// A agenda é montada em 3 passos:
//   1. Cursos em que o aluno está inscrito (enrollments, status ativo)
//   2. Encontros (activities) desses cursos, com título/local do curso
//   3. Presenças (attendances) do aluno nesses encontros -> flag "attended"

const TIMEZONE = 'America/Sao_Paulo';

// O schema atual guarda cada encontro como um único TIMESTAMPTZ
// (activities.activity_date), sem horário de término. Convertemos para
// o formato que a agenda expõe: uma data (YYYY-MM-DD) e um horário de
// início (HH:MM:SS), sempre no fuso de São Paulo.
function formatActivityDateTime(isoTimestamp) {
  const date = new Date(isoTimestamp);

  const activity_date = new Intl.DateTimeFormat('en-CA', {
    timeZone: TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);

  const start_time = new Intl.DateTimeFormat('en-GB', {
    timeZone: TIMEZONE,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date);

  return { activity_date, start_time };
}

/**
 * GET /api/agenda
 * Retorna os encontros dos cursos em que o aluno logado está inscrito,
 * ordenados por data/hora do encontro (activity_date, crescente).
 * Aceita o query param opcional "from" (YYYY-MM-DD) para filtrar
 * activity_date >= from.
 */
async function listAgenda(req, res) {
  try {
    const userId = req.userId;
    const { from } = req.query;

    // 1. Cursos em que o aluno está inscrito (ignora inscrições canceladas)
    const { data: enrollments, error: enrollmentsError } = await req.supabase
      .from('enrollments')
      .select('course_id')
      .eq('user_id', userId)
      .in('status', ['enrolled', 'attended']);

    if (enrollmentsError) {
      return res.status(400).json({ error: enrollmentsError.message });
    }

    const courseIds = [...new Set((enrollments || []).map((e) => e.course_id))];

    if (courseIds.length === 0) {
      return res.json([]);
    }

    // 2. Encontros (activities) desses cursos
    let activitiesQuery = req.supabase
      .from('activities')
      .select('id, title, activity_date, courses(title, location)')
      .in('course_id', courseIds)
      .order('activity_date', { ascending: true });

    if (from) {
      activitiesQuery = activitiesQuery.gte('activity_date', from);
    }

    const { data: activities, error: activitiesError } = await activitiesQuery;

    if (activitiesError) {
      return res.status(400).json({ error: activitiesError.message });
    }

    const activityIds = (activities || []).map((a) => a.id);

    // 3. Presenças do aluno nesses encontros (para marcar "attended")
    let attendedIds = new Set();
    if (activityIds.length > 0) {
      const { data: attendances, error: attendancesError } = await req.supabase
        .from('attendances')
        .select('activity_id')
        .eq('user_id', userId)
        .in('activity_id', activityIds);

      if (attendancesError) {
        return res.status(400).json({ error: attendancesError.message });
      }

      attendedIds = new Set((attendances || []).map((a) => a.activity_id));
    }

    // 4. Monta a resposta no formato consumido pelo app
    const agenda = (activities || []).map((activity) => {
      const { activity_date, start_time } = formatActivityDateTime(activity.activity_date);
      const course = Array.isArray(activity.courses) ? activity.courses[0] : activity.courses;

      return {
        activity_date,
        start_time,
        end_time: null, // o schema atual não guarda horário de término do encontro
        course_title: course ? course.title : null,
        title: activity.title,
        location: course ? course.location : null,
        attended: attendedIds.has(activity.id),
      };
    });

    res.json(agenda);
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
}

module.exports = { listAgenda };
