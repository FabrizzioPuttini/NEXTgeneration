// src/routes/perfil.routes.js
const express = require('express');
const router = express.Router();
const { getPerfilAluno } = require('../controllers/perfil.controller');
const authMiddleware = require('../middleware/auth');

// Rota para buscar o perfil do aluno logado
router.get('/', authMiddleware, getPerfilAluno);

module.exports = router;