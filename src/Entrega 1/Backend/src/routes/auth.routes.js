const express = require('express');

const {
  cadastrar,
  login
} = require('../controllers/auth.controller');

const router = express.Router();

router.post('/cadastro', cadastrar);

router.post('/login', login);

module.exports = router;