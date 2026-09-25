const express = require('express');
const router = express.Router();
const perfilController = require('../controllers/perfil.controller');
const authMiddleware = require('../middleware/auth'); // Seu verificador de token

// Rota GET /api/perfil
router.get('/', authMiddleware, perfilController.getPerfilAluno);

module.exports = router;