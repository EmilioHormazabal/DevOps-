require('dotenv').config()
const express = require('express')
const cors = require('cors')

const ventasRouter    = require('./routes/ventas.routes')
const despachosRouter = require('./routes/despachos.routes')

const app  = express()

app.use(cors())
app.use(express.json())

app.get('/health', (_req, res) => {
  res.json({
    status : 'ok',
    service: 'api-innovatech',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  })
})

app.use('/api/v1/ventas',    ventasRouter)
app.use('/api/v1/despachos', despachosRouter)

app.use((_req, res) => {
  res.status(404).json({ error: 'Ruta no encontrada' })
})

app.use((err, _req, res, _next) => {
  console.error('[ERROR]', err.message)
  res.status(500).json({ error: 'Error interno del servidor' })
})

module.exports = app
