package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.MedicoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.repository.MedicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private MedicoService medicoService;

    private MedicoRequest request;
    private Medico medico;

    @BeforeEach
    void setUp() {
        request = new MedicoRequest("Dr. House", "123456", "Cardiologia");

        medico = new Medico("Dr. House", "123456", "Cardiologia");
        medico.setId(1L);
    }

    @Test
    public void deveCriarMedicoQuandoCrmNaoExiste() {
        when(medicoRepository.existsByCrm("123456")).thenReturn(false);
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        Medico criado = medicoService.criar(request);

        assertEquals("Dr. House", criado.getNome());
        assertEquals("123456", criado.getCrm());
        assertEquals("Cardiologia", criado.getEspecialidade());
        verify(medicoRepository).save(any(Medico.class));
    }

    @Test
    public void naoDeveCriarMedicoComCrmJaExistente() {
        when(medicoRepository.existsByCrm("123456")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> medicoService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(medicoRepository, never()).save(any());
    }

    @Test
    public void deveListarTodosOsMedicos() {
        when(medicoRepository.findAll()).thenReturn(List.of(medico));

        List<Medico> resultado = medicoService.listarMedicos();

        assertEquals(1, resultado.size());
        assertEquals("Dr. House", resultado.get(0).getNome());
    }

    @Test
    public void deveBuscarMedicoPorIdExistente() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));

        Medico encontrado = medicoService.buscarMedicoPorId(1L);

        assertEquals(1L, encontrado.getId());
        assertEquals("Dr. House", encontrado.getNome());
    }

    @Test
    public void deveRetornar404AoBuscarMedicoInexistente() {
        when(medicoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> medicoService.buscarMedicoPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void deveAtualizarMedicoComDadosValidos() {
        MedicoRequest atualizacao = new MedicoRequest("Dr. Wilson", "654321", "Oncologia");

        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(medicoRepository.existsByCrmAndIdNot("654321", 1L)).thenReturn(false);
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        Medico atualizado = medicoService.atualizar(1L, atualizacao);

        assertEquals("Dr. Wilson", atualizado.getNome());
        assertEquals("654321", atualizado.getCrm());
        assertEquals("Oncologia", atualizado.getEspecialidade());
        verify(medicoRepository).save(medico);
    }

    @Test
    public void naoDeveAtualizarParaCrmDeOutroMedico() {
        MedicoRequest atualizacao = new MedicoRequest("Dr. Wilson", "999999", "Oncologia");

        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));
        when(medicoRepository.existsByCrmAndIdNot("999999", 1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> medicoService.atualizar(1L, atualizacao));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(medicoRepository, never()).save(any());
    }

    @Test
    public void naoDeveAtualizarMedicoInexistente() {
        when(medicoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> medicoService.atualizar(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(medicoRepository, never()).save(any());
    }

    @Test
    public void deveDeletarMedicoExistente() {
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(medico));

        medicoService.deletar(1L);

        verify(medicoRepository).delete(medico);
    }

    @Test
    public void naoDeveDeletarMedicoInexistente() {
        when(medicoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> medicoService.deletar(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(medicoRepository, never()).delete(any());
    }
}
