// Ponto de entrada do backend.
// Sobe o servidor Express, carrega as variáveis de ambiente e registra
// as rotas da API.
require('dotenv').config();

const express = require('express');
const cors = require('cors');

const agendaRoutes = require('./routes/agenda.routes');
const perfilRoutes = require('./routes/perfil.routes');
const cursoRoutes = require('./routes/curso.routes');
const authRoutes = require('./routes/auth.routes');
const inicioRoutes = require('./routes/inicio.routes');

const app = express();

// CORS liberado para o app Android conseguir consumir a API.
app.use(cors());

// Faz o parse do body das requisições como JSON.
app.use(express.json());

// Rota de health check, sem autenticação. Usada pelo Render para
// verificar se o serviço está no ar.
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Registro das rotas da API. A rota já aplica o middleware de
// autenticação internamente.
app.use('/api/agenda', agendaRoutes);
app.use('/api/perfil', perfilRoutes);
app.use('/api/cursos', cursoRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/inicio', inicioRoutes);

// O Render define a porta via variável de ambiente PORT; localmente
// caímos no 3000 como padrão.
const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Servidor rodando na porta ${PORT}`);
});
