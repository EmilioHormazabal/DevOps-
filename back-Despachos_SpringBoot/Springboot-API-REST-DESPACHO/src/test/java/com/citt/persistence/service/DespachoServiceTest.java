package com.citt.persistence.service;

import com.citt.exceptions.DespachoNotFoundException;
import com.citt.persistence.entity.Despacho;
import com.citt.persistence.repository.DespachoRepository;
import com.citt.persistence.services.DespachoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @InjectMocks
    private DespachoServiceImpl despachoService;

    private Despacho despacho;

    @BeforeEach
    public void setUp() {
        despacho = new Despacho();
        despacho.setIdDespacho(1L);
        despacho.setFechaDespacho(LocalDate.of(2025, 4, 14));
        despacho.setPatenteCamion("ABC-123");
        despacho.setIntento(1);
        despacho.setIdCompra(1L);
        despacho.setDireccionCompra("Calle Falsa 123");
        despacho.setValorCompra(1000L);
        despacho.setDespachado(false);
    }

    @Test
    @DisplayName("Cuando se guarda un despacho valido, entonces se persiste correctamente")
    public void whenSavingValidDespacho_thenItIsPersistedCorrectly() {
        when(despachoRepository.save(any(Despacho.class))).thenReturn(despacho);

        Despacho savedDespacho = despachoService.saveDespacho(despacho);

        verify(despachoRepository, times(1)).save(despacho);
        assertNotNull(savedDespacho);
        assertEquals(despacho.getDireccionCompra(), savedDespacho.getDireccionCompra());
        assertEquals(despacho.getPatenteCamion(), savedDespacho.getPatenteCamion());
        assertEquals(despacho.getValorCompra(), savedDespacho.getValorCompra());
        assertEquals(despacho.isDespachado(), savedDespacho.isDespachado());
    }

    @Test
    @DisplayName("Cuando se busca un despacho por ID existente, entonces se retorna correctamente")
    public void whenFindingDespachoById_thenReturnsDespacho() throws DespachoNotFoundException {
        when(despachoRepository.findById(1L)).thenReturn(Optional.of(despacho));

        Despacho found = despachoService.findById(1L);

        verify(despachoRepository, times(1)).findById(1L);
        assertNotNull(found);
        assertEquals(despacho.getIdDespacho(), found.getIdDespacho());
        assertEquals(despacho.getDireccionCompra(), found.getDireccionCompra());
    }
}
