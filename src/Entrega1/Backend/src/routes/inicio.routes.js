const express = require('express');
const router = express.Router();
const inicioController = require('../controllers/inicio.controller');
const authMiddleware = require('../middleware/auth');

router.get('/', authMiddleware, inicioController.getDadosInicio);

module.exports = router;