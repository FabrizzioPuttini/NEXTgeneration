export function qrState(qr, now = Date.now()) {
  const start = Date.parse(qr?.opensAt);
  const end = Date.parse(qr?.expiresAt);
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
    return { active: false, seconds: 0, label: 'Código indisponível' };
  }
  if (now < start) return { active: false, seconds: 0, label: 'Aguardando abertura' };
  if (now >= end) return { active: false, seconds: 0, label: 'Código expirado' };
  const seconds = Math.ceil((end - now) / 1000);
  return { active: true, seconds, label: `Válido por ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` };
}

export class ApiError extends Error {
  constructor(message, status = 0, code = 'REQUEST_FAILED') {
    super(message); this.name = 'ApiError'; this.status = status; this.code = code;
  }
}

const messages = {
  400: 'Confira os dados informados e tente novamente.',
  401: 'Sua sessão terminou. Entre novamente.',
  403: 'Esta conta não tem permissão para realizar a ação.',
  404: 'O registro não foi encontrado. Atualize a página.',
  409: 'A operação não está disponível para este registro.',
  410: 'O código expirou. Gere um novo QR Code.',
  429: 'Muitas tentativas. Aguarde um momento e tente novamente.',
};

/** A sessão vive apenas nesta closure. Logout invalida também respostas em andamento. */
export function createApiClient({ fetcher = globalThis.fetch, onSessionEnd = () => {} } = {}) {
  let session = null;
  let generation = 0;
  let refreshing = null;
  const assertCurrent = expected => {
    if (expected !== generation) throw new ApiError('A sessão foi alterada.', 401, 'SESSION_CHANGED');
  };
  function logout() {
    generation++; session = null; refreshing = null; onSessionEnd();
  }
  async function send(path, { method = 'GET', body, token } = {}) {
    if (!/^\/api\/[A-Za-z0-9_/?=&%.-]+$/.test(path) || path.includes('..')) {
      throw new ApiError('Destino da API inválido.', 400, 'INVALID_DESTINATION');
    }
    let result;
    try {
      result = await fetcher(path, {
        method, credentials: 'omit', cache: 'no-store', redirect: 'error',
        headers: { Accept: 'application/json', ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
      });
    } catch {
      throw new ApiError('Não foi possível conectar. Confira a internet e tente novamente.', 0, 'NETWORK_ERROR');
    }
    let data;
    try { data = await result.json(); } catch { data = null; }
    if (!result.ok) throw new ApiError(messages[result.status] || 'O serviço está indisponível. Tente novamente em instantes.', result.status, data?.code || 'REQUEST_FAILED');
    if (data === null) throw new ApiError('O servidor retornou uma resposta inválida.', 502, 'INVALID_RESPONSE');
    return data;
  }
  const validSession = value => typeof value?.access_token === 'string' && value.access_token.length > 0
    && typeof value?.refresh_token === 'string' && value.refresh_token.length > 0 && typeof value?.user?.id === 'string';
  async function login(email, senha) {
    const expected = ++generation;
    session = null; refreshing = null;
    const value = await send('/api/auth/login', { method: 'POST', body: { email: email.trim(), senha } });
    assertCurrent(expected);
    if (!validSession(value)) throw new ApiError('Não foi possível iniciar a sessão.', 502, 'INVALID_SESSION');
    session = value;
    return { id: value.user.id };
  }
  async function renew(expected) {
    assertCurrent(expected);
    if (!refreshing) {
      const previous = session;
      const promise = (async () => {
        try {
          const value = await send('/api/auth/refresh', { method: 'POST', body: { refresh_token: previous.refresh_token } });
          assertCurrent(expected);
          if (!validSession(value) || value.user.id !== previous.user.id) {
            logout(); throw new ApiError(messages[401], 401, 'UNAUTHENTICATED');
          }
          session = value;
        } catch (error) {
          if (expected === generation && error.status === 401) logout();
          throw error;
        }
      })();
      refreshing = promise;
      promise.finally(() => { if (refreshing === promise) refreshing = null; }).catch(() => {});
    }
    return refreshing;
  }
  async function request(path, options = {}) {
    if (!session) throw new ApiError(messages[401], 401, 'UNAUTHENTICATED');
    const expected = generation;
    const access = session.access_token;
    let result;
    try {
      result = await send(path, { ...options, token: access });
    } catch (error) {
      assertCurrent(expected);
      if (error.status !== 401) throw error;
      if (session.access_token === access) await renew(expected);
      assertCurrent(expected);
      try { result = await send(path, { ...options, token: session.access_token }); }
      catch (retryError) {
        assertCurrent(expected);
        if (retryError.status === 401) logout();
        throw retryError;
      }
    }
    assertCurrent(expected);
    return result;
  }
  return { login, request, logout, isAuthenticated: () => session !== null };
}
