const request = require('supertest')
const app = require('../app')

describe('GET /health', () => {
  it('responde con status ok', async () => {
    const res = await request(app).get('/health')
    expect(res.statusCode).toBe(200)
    expect(res.body).toHaveProperty('status', 'ok')
    expect(res.body).toHaveProperty('service', 'api-innovatech')
  })
})

describe('GET /api/v1/ventas', () => {
  it('responde con lista de ventas', async () => {
    const res = await request(app).get('/api/v1/ventas')
    expect(res.statusCode).toBe(200)
    expect(res.body).toHaveProperty('total')
    expect(Array.isArray(res.body.data)).toBe(true)
  })

  it('responde 404 para venta inexistente', async () => {
    const res = await request(app).get('/api/v1/ventas/999')
    expect(res.statusCode).toBe(404)
    expect(res.body).toHaveProperty('error')
  })
})

describe('POST /api/v1/ventas', () => {
  it('crea una venta nueva', async () => {
    const res = await request(app)
      .post('/api/v1/ventas')
      .send({ cliente: 'Test', producto: 'Item', cantidad: 1, precioUnit: 100 })
    expect(res.statusCode).toBe(201)
    expect(res.body).toHaveProperty('id')
    expect(res.body.cliente).toBe('Test')
  })

  it('responde 400 si faltan campos', async () => {
    const res = await request(app)
      .post('/api/v1/ventas')
      .send({ cliente: 'Test' })
    expect(res.statusCode).toBe(400)
  })
})

describe('GET /api/v1/despachos', () => {
  it('responde con lista de despachos', async () => {
    const res = await request(app).get('/api/v1/despachos')
    expect(res.statusCode).toBe(200)
    expect(res.body).toHaveProperty('total')
    expect(Array.isArray(res.body.data)).toBe(true)
  })
})

describe('POST /api/v1/despachos', () => {
  it('crea un despacho nuevo', async () => {
    const res = await request(app)
      .post('/api/v1/despachos')
      .send({ ventaId: 1, direccion: 'Calle 123' })
    expect(res.statusCode).toBe(201)
    expect(res.body).toHaveProperty('id')
    expect(res.body.estado).toBe('pendiente')
  })

  it('responde 400 si falta direccion', async () => {
    const res = await request(app)
      .post('/api/v1/despachos')
      .send({ ventaId: 1 })
    expect(res.statusCode).toBe(400)
  })
})

describe('404 handler', () => {
  it('responde 404 para rutas inexistentes', async () => {
    const res = await request(app).get('/ruta-inexistente')
    expect(res.statusCode).toBe(404)
  })
})
