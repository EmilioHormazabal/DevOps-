import { render, screen } from '@testing-library/react'
import Navbar from '../componentes/Layouts/Navbar'

describe('Navbar', () => {
  it('renderiza el titulo del dashboard', () => {
    render(<Navbar />)
    expect(screen.getByText('Despacho Dashboard')).toBeInTheDocument()
  })

  it('renderiza los enlaces de navegacion', () => {
    render(<Navbar />)
    expect(screen.getByText('Usuarios')).toBeInTheDocument()
    expect(screen.getByText('Productos')).toBeInTheDocument()
    expect(screen.getByText('Configuración')).toBeInTheDocument()
  })
})
