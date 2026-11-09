import { createApiClient, qrState } from './admin-core.mjs';

const $ = id => document.getElementById(id);
const state = { courses: [], meetings: [], messages: [], meeting: null, conversation: null, editingCourse: null, editingMeeting: null, qr: null };
const versions = { meetings: 0, courses: 0, attendance: 0, messages: 0, announcements: 0, editor: 0 };
const formatDate = value => {
  const date = new Date(value);
  return Number.isFinite(date.getTime()) ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(date) : 'Data não informada';
};
const localDateInput = value => {
  if (!value) return '';
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return '';
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
const node = (tag, text, className) => {
  const element = document.createElement(tag);
  if (text !== undefined) element.textContent = text;
  if (className) element.className = className;
  return element;
};
function status(id, text = '', kind = '') {
  $(id).textContent = text;
  $(id).className = `status ${id === 'global-status' ? 'global-status' : ''} ${kind}`;
}
function placeholder(id, text, loading = false) {
  $(id).replaceChildren(node('p', text, loading ? 'loading' : 'list-empty'));
  $(id).setAttribute('aria-busy', String(loading));
}
function errorStatus(error, id = 'global-status') {
  if (error.code === 'SESSION_CHANGED') return;
  if (!api.isAuthenticated() && id !== 'login-status') id = 'login-status';
  status(id, error.message || 'Não foi possível concluir. Tente novamente.', 'error');
}
async function busy(element, work) {
  if (element.dataset.busy === 'true') return;
  element.dataset.busy = 'true';
  element.setAttribute('aria-busy', 'true');
  const controls = element.matches('button') ? [element] : [...element.querySelectorAll('input,textarea,select,button')];
  const previous = controls.map(control => control.disabled);
  controls.forEach(control => { control.disabled = true; });
  try { await work(); }
  finally {
    controls.forEach((control, index) => { control.disabled = previous[index]; });
    element.dataset.busy = 'false'; element.setAttribute('aria-busy', 'false');
    updateQrControls();
  }
}
function showEditor(id, focusId) {
  $(id).hidden = false;
  $(id).scrollIntoView({ block: 'nearest' });
  $(focusId).focus({ preventScroll: true });
}
function resetPrivateView() {
  Object.keys(versions).forEach(key => { versions[key]++; });
  state.courses = []; state.meetings = []; state.messages = [];
  state.meeting = null; state.conversation = null; state.editingCourse = null; state.editingMeeting = null;
  $('workspace').hidden = true; $('logout').hidden = true; $('login-view').hidden = false;
  ['course-editor', 'meeting-editor', 'conversation-detail', 'meeting-detail', 'cancel-meeting-confirm', 'archive-course-confirm'].forEach(id => { $(id).hidden = true; });
  ['course-list', 'meeting-list', 'attendance-list', 'announcement-list', 'conversation-list', 'message-list', 'meeting-course-input'].forEach(id => $(id).replaceChildren());
  ['meeting-name', 'meeting-course', 'meeting-date', 'conversation-title', 'attendance-count'].forEach(id => { $(id).textContent = ''; });
  ['login-form', 'course-form', 'meeting-form', 'announcement-form', 'reply-form'].forEach(id => $(id).reset());
  ['meeting-search', 'course-search'].forEach(id => { $(id).value = ''; });
  document.querySelectorAll('.status').forEach(element => { element.textContent = ''; });
  $('meeting-empty').hidden = false; $('conversation-empty').hidden = false;
  resetQr();
  status('login-status', 'Sessão encerrada. Entre para acessar o painel.');
}
const api = createApiClient({ onSessionEnd: resetPrivateView });
$('logout').addEventListener('click', () => { api.logout(); $('email').focus(); });
$('login-form').addEventListener('submit', event => {
  event.preventDefault();
  const email = $('email').value;
  const password = $('password').value;
  busy(event.currentTarget, async () => {
    status('login-status', 'Verificando seu acesso…');
    try {
      await api.login(email, password);
      const access = await api.request('/api/admin/me');
      if (access.admin !== true) throw new Error('Esta conta não tem acesso ao painel da equipe.');
      $('password').value = '';
      $('login-view').hidden = true; $('workspace').hidden = false; $('logout').hidden = false;
      status('login-status');
      await navigate('encontros');
      document.querySelector('[data-view="encontros"]').focus();
    } catch (error) {
      if (error.code === 'SESSION_CHANGED') return;
      if (api.isAuthenticated()) api.logout();
      $('password').value = '';
      status('login-status', error.status === 401 ? 'Não foi possível entrar. Confira seu e-mail e senha.' : error.message, 'error');
    }
  });
});

async function navigate(view) {
  if (!api.isAuthenticated()) return;
  document.querySelectorAll('[data-view]').forEach(button => {
    if (button.dataset.view === view) button.setAttribute('aria-current', 'page');
    else button.removeAttribute('aria-current');
  });
  document.querySelectorAll('.view').forEach(section => { section.hidden = section.id !== `view-${view}`; });
  status('global-status');
  const loaders = { encontros: loadMeetings, cursos: loadCourses, comunicados: loadAnnouncements, duvidas: loadMessages };
  await loaders[view]();
}
document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => navigate(button.dataset.view)));
function reload(buttonId, loader) {
  $(buttonId).addEventListener('click', event => busy(event.currentTarget, loader));
}

async function loadMeetings() {
  const version = ++versions.meetings;
  placeholder('meeting-list', 'Carregando encontros…', true);
  try {
    const rows = await api.request('/api/admin/encontros');
    if (version !== versions.meetings) return;
    if (!Array.isArray(rows)) throw new Error('Não foi possível ler os encontros.');
    state.meetings = rows;
    renderMeetings();
    if (state.meeting) {
      const selected = rows.find(row => row.id === state.meeting.id);
      if (selected) { state.meeting = selected; renderMeetingDetail(); }
      else clearMeeting();
    }
  } catch (error) {
    if (version !== versions.meetings) return;
    state.meetings = []; clearMeeting();
    placeholder('meeting-list', 'Não foi possível carregar. Use Atualizar para tentar novamente.');
    errorStatus(error);
  }
}
function renderMeetings() {
  const query = $('meeting-search').value.trim().toLocaleLowerCase('pt-BR');
  const rows = state.meetings.filter(row => `${row.title} ${row.course_title}`.toLocaleLowerCase('pt-BR').includes(query));
  $('meeting-list').replaceChildren(); $('meeting-list').setAttribute('aria-busy', 'false');
  if (!rows.length) return placeholder('meeting-list', query ? 'Nenhum encontro encontrado.' : 'Nenhum encontro cadastrado. Comece em Novo encontro.');
  for (const row of rows) {
    const button = node('button', undefined, 'selection-item'); button.type = 'button';
    button.setAttribute('aria-pressed', String(row.id === state.meeting?.id));
    button.append(node('span', row.title, 'item-title'), node('span', row.course_title || 'Curso', 'item-meta'), node('span', `${formatDate(row.activity_date)}${row.is_cancelled ? ' · Cancelado' : ''}`, 'item-meta'));
    button.addEventListener('click', () => selectMeeting(row));
    $('meeting-list').append(button);
  }
}
function clearMeeting() {
  state.meeting = null; versions.attendance++; resetQr();
  $('meeting-detail').hidden = true; $('meeting-empty').hidden = false;
  $('attendance-list').replaceChildren(); $('attendance-count').textContent = '';
}
function selectMeeting(row) {
  const changed = state.meeting?.id !== row.id;
  state.meeting = row;
  if (changed) resetQr();
  renderMeetings(); renderMeetingDetail();
  loadAttendance();
}
function renderMeetingDetail() {
  if (!state.meeting) return;
  const row = state.meeting;
  $('meeting-empty').hidden = true; $('meeting-detail').hidden = false;
  $('meeting-name').textContent = row.title;
  $('meeting-course').textContent = row.course_title || 'Curso';
  $('meeting-date').textContent = formatDate(row.activity_date);
  $('meeting-cancelled').hidden = !row.is_cancelled;
  if (row.is_cancelled) resetQr();
  updateQrControls();
}
function updateQrControls() {
  const disabled = !state.meeting || state.meeting.is_cancelled || $('qr-form').dataset.busy === 'true';
  $('qr-duration').disabled = Boolean(disabled);
  $('qr-form').querySelector('button').disabled = Boolean(disabled);
}
function resetQr() {
  state.qr = null;
  $('qr-image').hidden = true; $('qr-image').removeAttribute('src');
  $('qr-placeholder').hidden = false;
  const cancelled = Boolean(state.meeting?.is_cancelled);
  $('qr-placeholder').querySelector('p').textContent = cancelled ? 'Encontro cancelado' : 'Pronto para receber os alunos?';
  $('qr-placeholder').querySelector('small').textContent = cancelled ? 'O histórico de presenças permanece disponível abaixo.' : 'Gere o código quando o encontro começar.';
  $('qr-status').textContent = cancelled ? 'Check-in encerrado' : 'Check-in ainda não aberto'; $('qr-status').className = 'qr-status';
  $('qr-expiration').textContent = '';
}
function renderQr() {
  if (!state.qr) return;
  const current = qrState(state.qr);
  $('qr-status').textContent = current.label;
  $('qr-status').className = `qr-status${current.active ? '' : ' expired'}`;
  $('qr-image').hidden = !current.active;
  $('qr-placeholder').hidden = current.active;
  if (current.active) $('qr-image').src = state.qr.qrDataUrl;
  else {
    $('qr-image').removeAttribute('src');
    $('qr-placeholder').querySelector('p').textContent = current.label;
    $('qr-placeholder').querySelector('small').textContent = 'Gere um novo código para reabrir o check-in.';
  }
}
setInterval(renderQr, 1000);
$('qr-form').addEventListener('submit', event => {
  event.preventDefault();
  if (!state.meeting || state.meeting.is_cancelled) return;
  const id = state.meeting.id;
  const durationMinutes = Number($('qr-duration').value);
  busy(event.currentTarget, async () => {
    resetQr();
    status('global-status', 'Gerando QR Code…');
    try {
      const qr = await api.request(`/api/admin/encontros/${encodeURIComponent(id)}/qr`, { method: 'POST', body: { durationMinutes } });
      if (state.meeting?.id !== id) return;
      if (qr.activityId !== id || !/^data:image\/png;base64,[A-Za-z0-9+/=\r\n]+$/.test(qr.qrDataUrl || '')) throw new Error('O servidor não retornou um QR Code válido. Tente novamente.');
      state.qr = qr;
      $('qr-expiration').textContent = `Encerra em ${formatDate(qr.expiresAt)} · horário deste dispositivo`;
      renderQr(); status('global-status', 'QR Code gerado. Exiba-o aos alunos.', 'success');
    } catch (error) { errorStatus(error); }
  });
});
async function loadAttendance() {
  if (!state.meeting) return;
  const id = state.meeting.id;
  const version = ++versions.attendance;
  $('attendance-count').textContent = '';
  placeholder('attendance-list', 'Carregando presenças…', true);
  try {
    const rows = await api.request(`/api/admin/encontros/${encodeURIComponent(id)}/presencas`);
    if (version !== versions.attendance || state.meeting?.id !== id) return;
    if (!Array.isArray(rows)) throw new Error('Não foi possível ler as presenças.');
    $('attendance-list').replaceChildren(); $('attendance-list').setAttribute('aria-busy', 'false');
    $('attendance-count').textContent = `(${rows.length})`;
    if (!rows.length) return placeholder('attendance-list', 'Nenhuma presença confirmada ainda. Atualize após a leitura do QR Code.');
    for (const row of rows) {
      const item = node('div', undefined, 'attendance-row');
      const time = node('time', formatDate(row.scanned_at)); time.dateTime = row.scanned_at;
      item.append(node('span', row.nome || 'Aluno'), time); $('attendance-list').append(item);
    }
  } catch (error) {
    if (version !== versions.attendance || state.meeting?.id !== id) return;
    placeholder('attendance-list', 'Não foi possível carregar as presenças. Tente atualizar.'); errorStatus(error);
  }
}
reload('reload-meetings', loadMeetings); reload('reload-attendance', loadAttendance);
$('meeting-search').addEventListener('input', renderMeetings);

async function loadCourses() {
  const version = ++versions.courses;
  placeholder('course-list', 'Carregando cursos…', true);
  try {
    const rows = await api.request('/api/admin/cursos');
    if (version !== versions.courses) return;
    if (!Array.isArray(rows)) throw new Error('Não foi possível ler os cursos.');
    state.courses = rows; renderCourses();
  } catch (error) {
    if (version !== versions.courses) return;
    state.courses = []; placeholder('course-list', 'Não foi possível carregar. Use Atualizar para tentar novamente.'); errorStatus(error);
  }
}
function renderCourses() {
  const query = $('course-search').value.trim().toLocaleLowerCase('pt-BR');
  const rows = state.courses.filter(row => `${row.title} ${row.location}`.toLocaleLowerCase('pt-BR').includes(query));
  $('course-list').replaceChildren(); $('course-list').setAttribute('aria-busy', 'false');
  if (!rows.length) return placeholder('course-list', query ? 'Nenhum curso encontrado.' : 'Nenhum curso cadastrado. Comece em Novo curso.');
  for (const row of rows) {
    const item = node('article', undefined, 'course-row'); const info = node('div');
    const heading = node('h2', row.title);
    if (row.is_active === false) heading.append(node('span', 'Arquivado', 'tag'));
    info.append(heading, node('p', `${row.location || 'Local não informado'} · ${formatDate(row.course_date)}`), node('p', `${row.workload} ${Number(row.workload) === 1 ? 'hora' : 'horas'} · ${row.meetings_count} ${Number(row.meetings_count) === 1 ? 'encontro' : 'encontros'}`));
    const button = node('button', 'Editar', 'quiet'); button.type = 'button'; button.setAttribute('aria-label', `Editar curso ${row.title}`);
    button.addEventListener('click', () => editCourse(row)); item.append(info, button); $('course-list').append(item);
  }
}
function editCourse(row = null) {
  if ($('course-form').dataset.busy === 'true' || $('course-editor').dataset.busy === 'true') return;
  state.editingCourse = row?.id || null;
  $('course-form').reset(); status('course-form-status'); $('archive-course-confirm').hidden = true;
  $('course-editor-title').textContent = row ? 'Editar curso' : 'Novo curso';
  $('archive-course').hidden = !row || row.is_active === false;
  if (row) {
    $('course-title').value = row.title || ''; $('course-description').value = row.description || '';
    $('course-location').value = row.location || ''; $('course-date').value = localDateInput(row.course_date);
    $('course-workload').value = row.workload; $('course-meetings').value = row.meetings_count;
    $('course-spots').value = row.total_spots ?? 30;
    $('course-active').checked = row.is_active !== false;
  }
  showEditor('course-editor', 'course-title');
}
$('new-course').addEventListener('click', () => editCourse());
$('close-course-editor').addEventListener('click', () => { if ($('course-form').dataset.busy !== 'true') { $('course-editor').hidden = true; $('new-course').focus(); } });
$('course-form').addEventListener('submit', event => {
  event.preventDefault(); const id = state.editingCourse;
  const body = { title: $('course-title').value.trim(), description: $('course-description').value.trim(), location: $('course-location').value.trim(), course_date: new Date($('course-date').value).toISOString(), workload: Number($('course-workload').value), meetings_count: Number($('course-meetings').value), total_spots: Number($('course-spots').value), is_active: $('course-active').checked };
  busy(event.currentTarget, async () => {
    status('course-form-status', 'Salvando curso…');
    try {
      await api.request(`/api/admin/cursos${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PATCH' : 'POST', body });
      $('course-editor').hidden = true; status('global-status', 'Curso salvo.', 'success'); await loadCourses(); $('new-course').focus();
    } catch (error) { errorStatus(error, 'course-form-status'); }
  });
});
$('archive-course').addEventListener('click', () => { $('archive-course-confirm').hidden = false; $('confirm-archive-course').focus(); });
$('keep-course').addEventListener('click', () => { $('archive-course-confirm').hidden = true; $('archive-course').focus(); });
$('confirm-archive-course').addEventListener('click', () => {
  const id = state.editingCourse;
  if (!id) return;
  busy($('course-editor'), async () => {
    try {
      await api.request(`/api/admin/cursos/${encodeURIComponent(id)}`, { method: 'DELETE' });
      $('course-editor').hidden = true; $('archive-course-confirm').hidden = true;
      status('global-status', 'Curso arquivado. O histórico foi preservado.', 'success'); await loadCourses(); $('new-course').focus();
    } catch (error) { errorStatus(error, 'course-form-status'); }
  });
});
reload('reload-courses', loadCourses); $('course-search').addEventListener('input', renderCourses);

async function editMeeting(row = null) {
  if ($('meeting-form').dataset.busy === 'true' || $('meeting-editor').dataset.busy === 'true') return;
  const version = ++versions.editor;
  state.editingMeeting = row?.id || null;
  $('meeting-form').reset(); status('meeting-form-status', 'Carregando cursos…'); $('cancel-meeting-confirm').hidden = true;
  $('meeting-editor-title').textContent = row ? 'Editar encontro' : 'Novo encontro';
  $('cancel-meeting').hidden = !row || row.is_cancelled;
  $('meeting-title').value = row?.title || ''; $('meeting-date-input').value = localDateInput(row?.activity_date);
  $('meeting-course-input').replaceChildren();
  $('meeting-course-input').disabled = Boolean(row);
  $('meeting-course-input').title = row ? 'O curso fica fixo para preservar o histórico de presenças.' : '';
  showEditor('meeting-editor', 'meeting-title');
  await busy($('meeting-form'), async () => {
    try {
      const courses = await api.request('/api/admin/cursos');
      if (version !== versions.editor) return;
      if (!Array.isArray(courses)) throw new Error('Não foi possível ler os cursos.');
      for (const course of courses.filter(item => item.is_active !== false || item.id === row?.course_id)) {
        const option = node('option', course.title); option.value = course.id; $('meeting-course-input').append(option);
      }
      if (row) $('meeting-course-input').value = row.course_id;
      if (!$('meeting-course-input').options.length) status('meeting-form-status', 'Cadastre um curso ativo na área Cursos antes de criar um encontro.', 'error');
      else { status('meeting-form-status'); $('meeting-title').focus(); }
    } catch (error) { errorStatus(error, 'meeting-form-status'); }
  });
}
$('new-meeting').addEventListener('click', () => editMeeting());
$('edit-meeting').addEventListener('click', () => editMeeting(state.meeting));
$('close-meeting-editor').addEventListener('click', () => { if ($('meeting-form').dataset.busy !== 'true') { versions.editor++; $('meeting-editor').hidden = true; $('new-meeting').focus(); } });
$('meeting-form').addEventListener('submit', event => {
  event.preventDefault(); const id = state.editingMeeting;
  const body = { title: $('meeting-title').value.trim(), activity_date: new Date($('meeting-date-input').value).toISOString() };
  if (!id) body.course_id = $('meeting-course-input').value;
  busy(event.currentTarget, async () => {
    status('meeting-form-status', 'Salvando encontro…');
    try {
      const saved = await api.request(`/api/admin/encontros${id ? `/${encodeURIComponent(id)}` : ''}`, { method: id ? 'PATCH' : 'POST', body });
      $('meeting-editor').hidden = true; status('global-status', 'Encontro salvo.', 'success');
      await loadMeetings();
      const selected = state.meetings.find(item => item.id === saved.id);
      if (selected) selectMeeting(selected);
      $('new-meeting').focus();
    } catch (error) { errorStatus(error, 'meeting-form-status'); }
  });
});
$('cancel-meeting').addEventListener('click', () => { $('cancel-meeting-confirm').hidden = false; $('confirm-cancel-meeting').focus(); });
$('keep-meeting').addEventListener('click', () => { $('cancel-meeting-confirm').hidden = true; $('cancel-meeting').focus(); });
$('confirm-cancel-meeting').addEventListener('click', () => {
  const id = state.editingMeeting;
  if (!id) return;
  busy($('meeting-editor'), async () => {
    try {
      await api.request(`/api/admin/encontros/${encodeURIComponent(id)}`, { method: 'DELETE' });
      $('meeting-editor').hidden = true; $('cancel-meeting-confirm').hidden = true;
      if (state.meeting?.id === id) resetQr();
      status('global-status', 'Encontro cancelado. O histórico foi preservado.', 'success'); await loadMeetings(); $('new-meeting').focus();
    } catch (error) { errorStatus(error, 'meeting-form-status'); }
  });
});

async function loadAnnouncements() {
  const version = ++versions.announcements;
  placeholder('announcement-list', 'Carregando comunicados…', true);
  try {
    const rows = await api.request('/api/comunicados');
    if (version !== versions.announcements) return;
    if (!Array.isArray(rows)) throw new Error('Não foi possível ler os comunicados.');
    $('announcement-list').replaceChildren(); $('announcement-list').setAttribute('aria-busy', 'false');
    if (!rows.length) return placeholder('announcement-list', 'Nenhum comunicado publicado.');
    for (const row of rows) {
      const item = node('article', undefined, 'announcement');
      const time = node('time', formatDate(row.created_at)); time.dateTime = row.created_at;
      item.append(node('h3', row.titulo), time, node('p', row.conteudo)); $('announcement-list').append(item);
    }
  } catch (error) {
    if (version !== versions.announcements) return;
    placeholder('announcement-list', 'Não foi possível carregar os comunicados. Tente atualizar.'); errorStatus(error);
  }
}
$('announcement-form').addEventListener('submit', event => {
  event.preventDefault();
  const body = { titulo: $('announcement-title').value.trim(), conteudo: $('announcement-body').value.trim() };
  busy(event.currentTarget, async () => {
    status('announcement-status', 'Publicando comunicado…');
    try {
      await api.request('/api/admin/comunicados', { method: 'POST', body });
      $('announcement-form').reset(); status('announcement-status', 'Comunicado publicado para os alunos.', 'success'); await loadAnnouncements();
    } catch (error) { errorStatus(error, 'announcement-status'); }
  });
});
reload('reload-announcements', loadAnnouncements);

function conversations() {
  const groups = new Map();
  for (const message of state.messages) {
    if (!groups.has(message.user_id)) groups.set(message.user_id, { id: message.user_id, name: message.nome || `Aluno ${String(message.user_id).slice(0, 8)}`, messages: [] });
    groups.get(message.user_id).messages.push(message);
  }
  for (const group of groups.values()) group.messages.sort((a, b) => Date.parse(a.created_at) - Date.parse(b.created_at));
  return [...groups.values()].sort((a, b) => Date.parse(b.messages.at(-1).created_at) - Date.parse(a.messages.at(-1).created_at));
}
async function loadMessages() {
  const version = ++versions.messages;
  placeholder('conversation-list', 'Carregando conversas…', true);
  try {
    const rows = await api.request('/api/admin/mensagens');
    if (version !== versions.messages) return;
    if (!Array.isArray(rows)) throw new Error('Não foi possível ler as mensagens.');
    state.messages = rows; renderConversations(); renderConversation();
  } catch (error) {
    if (version !== versions.messages) return;
    state.messages = []; state.conversation = null; renderConversation();
    placeholder('conversation-list', 'Não foi possível carregar. Use Atualizar mensagens.'); errorStatus(error);
  }
}
function renderConversations() {
  const groups = conversations();
  $('conversation-list').replaceChildren(); $('conversation-list').setAttribute('aria-busy', 'false');
  if (!groups.length) return placeholder('conversation-list', 'As dúvidas enviadas pelos alunos aparecerão aqui.');
  for (const group of groups) {
    const button = node('button', undefined, 'selection-item'); button.type = 'button';
    button.setAttribute('aria-pressed', String(group.id === state.conversation));
    button.append(node('span', group.name, 'item-title'), node('span', formatDate(group.messages.at(-1).created_at), 'item-meta'));
    button.addEventListener('click', () => {
      if ($('reply-form').dataset.busy === 'true') return;
      if (state.conversation !== group.id) { $('reply-form').reset(); status('reply-status'); }
      state.conversation = group.id; renderConversations(); renderConversation();
    });
    $('conversation-list').append(button);
  }
}
function renderConversation() {
  const group = conversations().find(item => item.id === state.conversation);
  $('conversation-empty').hidden = Boolean(group); $('conversation-detail').hidden = !group;
  $('message-list').replaceChildren();
  if (!group) { state.conversation = null; $('conversation-title').textContent = ''; return; }
  $('conversation-title').textContent = group.name;
  for (const message of group.messages) {
    const team = message.remetente === 'equipe';
    const item = node('article', undefined, `message${team ? ' team' : ''}`);
    const time = node('time', formatDate(message.created_at)); time.dateTime = message.created_at;
    item.append(node('div', team ? 'Equipe Próxima Etapa' : group.name, 'sender'), node('p', message.conteudo), time);
    $('message-list').append(item);
  }
  $('message-list').scrollTop = $('message-list').scrollHeight;
}
$('reply-form').addEventListener('submit', event => {
  event.preventDefault();
  const userId = state.conversation;
  if (!userId) return;
  const conteudo = $('reply-body').value.trim();
  busy(event.currentTarget, async () => {
    status('reply-status', 'Enviando resposta…');
    try {
      await api.request('/api/admin/mensagens', { method: 'POST', body: { userId, conteudo } });
      if (state.conversation === userId) { $('reply-form').reset(); status('reply-status', 'Resposta enviada.', 'success'); }
      await loadMessages();
    } catch (error) { errorStatus(error, 'reply-status'); }
  });
});
reload('reload-messages', loadMessages);
