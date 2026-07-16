const app = require('./app')
const PORT = process.env.PORT || 3000

app.listen(PORT, () => {
  console.log(`API Innovatech corriendo en http://localhost:${PORT}`)
  console.log(`Entorno: ${process.env.NODE_ENV || 'development'}`)
})
