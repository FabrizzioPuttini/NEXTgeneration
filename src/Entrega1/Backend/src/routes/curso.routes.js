// src/routes/curso.routes.js
const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const cursoController = require('../controllers/curso.controller');

// Rota para listar todos os cursos (GET /api/cursos)
router.get('/', cursoController.listarCursos);

// Rota para buscar um curso específico (GET /api/cursos/:id)
router.get('/:id', cursoController.buscarCursoPorId);

// Rota para inscrever-se em um curso (POST /api/cursos/:id/inscricao)
router.post('/:id/inscricao', auth, cursoController.inscreverCurso);

module.exports = router;
