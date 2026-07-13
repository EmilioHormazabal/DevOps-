package persistence.service;

import com.citt.exceptions.VentaNotFoundException;
import com.citt.persistence.entity.Venta;
import com.citt.persistence.repository.VentaRepository;
import com.citt.persistence.services.VentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Venta venta;

    @BeforeEach
    public void setUp() {
        venta = Venta.builder()
                .idVenta(1L)
                .direccionCompra("Calle Falsa 123")
                .valorCompra(1000)
                .fechaCompra(LocalDate.of(2025, 4, 14))
                .despachoGenerado(false)
                .build();
    }

    @Test
    @DisplayName("Cuando se guarda una venta válida, entonces se persiste correctamente")
    public void whenSavingValidVenta_thenItIsPersistedCorrectly() {
        when(ventaRepository.save(any(Venta.class))).thenReturn(venta);

        Venta savedVenta = ventaService.saveVenta(venta);

        verify(ventaRepository, times(1)).save(venta);
        assertNotNull(savedVenta);
        assertEquals(venta.getDireccionCompra(), savedVenta.getDireccionCompra());
        assertEquals(venta.getValorCompra(), savedVenta.getValorCompra());
    }

    @Test
    @DisplayName("Cuando se guarda una venta, entonces se asigna un ID")
    public void whenVentaIsSaved_thenIdIsAssigned() {
        Venta ventaToSave = Venta.builder()
                .direccionCompra("Calle Falsa 123")
                .valorCompra(1000)
                .fechaCompra(LocalDate.of(2025, 4, 14))
                .despachoGenerado(false)
                .build();

        Venta ventaWithId = Venta.builder()
                .idVenta(1L)
                .direccionCompra("Calle Falsa 123")
                .valorCompra(1000)
                .fechaCompra(LocalDate.of(2025, 4, 14))
                .despachoGenerado(false)
                .build();

        when(ventaRepository.save(any(Venta.class))).thenReturn(ventaWithId);

        Venta result = ventaService.saveVenta(ventaToSave);

        verify(ventaRepository).save(ventaToSave);
        assertNotNull(result);
        assertEquals(1L, result.getIdVenta());
    }

    @Test
    @DisplayName("Cuando se busca una venta por ID existente, entonces la retorna")
    public void whenFindById_thenReturnVenta() throws VentaNotFoundException {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        Venta found = ventaService.findById(1L);

        assertNotNull(found);
        assertEquals(venta.getIdVenta(), found.getIdVenta());
    }

    @Test
    @DisplayName("Cuando se busca una venta por ID inexistente, entonces lanza excepción")
    public void whenFindById_thenThrowException() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VentaNotFoundException.class, () -> ventaService.findById(99L));
    }

    @Test
    @DisplayName("Cuando se actualiza una venta existente, entonces retorna la venta actualizada")
    public void whenUpdateVenta_thenReturnUpdatedVenta() throws VentaNotFoundException {
        Venta updated = Venta.builder()
                .idVenta(1L)
                .direccionCompra("Nueva Direccion 456")
                .valorCompra(2000)
                .fechaCompra(LocalDate.of(2025, 5, 1))
                .despachoGenerado(true)
                .build();

        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenReturn(updated);

        Venta result = ventaService.updateVenta(1L, updated);

        assertNotNull(result);
        assertEquals("Nueva Direccion 456", result.getDireccionCompra());
        assertEquals(2000, result.getValorCompra());
    }

    @Test
    @DisplayName("Cuando se actualiza una venta inexistente, entonces lanza excepción")
    public void whenUpdateNonExistentVenta_thenThrowException() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VentaNotFoundException.class, () -> ventaService.updateVenta(99L, venta));
    }

    @Test
    @DisplayName("Cuando se elimina una venta existente, entonces no lanza excepción")
    public void whenDeleteVenta_thenSuccess() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        doNothing().when(ventaRepository).deleteById(1L);

        assertDoesNotThrow(() -> ventaService.deleteVenta(1L));
        verify(ventaRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Cuando se elimina una venta inexistente, entonces lanza excepción")
    public void whenDeleteNonExistentVenta_thenThrowException() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VentaNotFoundException.class, () -> ventaService.deleteVenta(99L));
    }

    @Test
    @DisplayName("Cuando se listan todas las ventas, entonces retorna la lista completa")
    public void whenFindAllVentas_thenReturnList() {
        Venta venta2 = Venta.builder()
                .idVenta(2L)
                .direccionCompra("Otra Direccion 789")
                .valorCompra(500)
                .fechaCompra(LocalDate.of(2025, 4, 15))
                .despachoGenerado(true)
                .build();

        when(ventaRepository.findAll()).thenReturn(Arrays.asList(venta, venta2));

        List<Venta> ventas = ventaService.findAllVentas();

        assertNotNull(ventas);
        assertEquals(2, ventas.size());
        verify(ventaRepository, times(1)).findAll();
    }
}
