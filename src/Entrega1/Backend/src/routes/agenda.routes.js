// Rotas de agenda.
const express = require('express');
const auth = require('../middleware/auth');
const { listAgenda } = require('../controllers/agenda.controller');

const router = express.Router();

// GET /api/agenda -> encontros do aluno logado (aceita ?from=)
router.get('/', auth, listAgenda);

module.exports = router;
