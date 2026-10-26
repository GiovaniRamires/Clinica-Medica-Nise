package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.PacienteRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.PacienteRepository;
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
public class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private PacienteRequest request;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        request = new PacienteRequest(
                "Maria Silva",
                "12345678901",
                "Unimed",
                "Nenhuma",
                "Prontuario inicial");

        paciente = new Paciente(
                "Maria Silva",
                "12345678901",
                "Unimed",
                "Nenhuma",
                "Prontuario inicial");
        paciente.setId(1L);
    }

    @Test
    public void deveCriarPacienteQuandoCpfNaoExiste() {
        when(pacienteRepository.existsByCpf("12345678901")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Paciente criado = pacienteService.criar(request);

        assertEquals("Maria Silva", criado.getNome());
        assertEquals("12345678901", criado.getCpf());
        assertEquals("Unimed", criado.getConvenio());
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    public void naoDeveCriarPacienteComCpfJaExistente() {
        when(pacienteRepository.existsByCpf("12345678901")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pacienteService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(pacienteRepository, never()).save(any());
    }


    @Test
    public void deveListarTodosOsPacientes() {
        when(pacienteRepository.findAll()).thenReturn(List.of(paciente));

        List<Paciente> resultado = pacienteService.listarPacientes();

        assertEquals(1, resultado.size());
        assertEquals("Maria Silva", resultado.get(0).getNome());
    }


    @Test
    public void deveBuscarPacientePorIdExistente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        Paciente encontrado = pacienteService.buscarPacientePorId(1L);

        assertEquals(1L, encontrado.getId());
        assertEquals("Maria Silva", encontrado.getNome());
    }

    @Test
    public void deveRetornar404AoBuscarPacienteInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pacienteService.buscarPacientePorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }


    @Test
    public void deveAtualizarPacienteComDadosValidos() {
        PacienteRequest atualizacao = new PacienteRequest(
                "Maria Souza",
                "12345678901",
                "Amil",
                "Penicilina",
                "Prontuario atualizado");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByCpfAndIdNot("12345678901", 1L)).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Paciente atualizado = pacienteService.atualizar(1L, atualizacao);

        assertEquals("Maria Souza", atualizado.getNome());
        assertEquals("Amil", atualizado.getConvenio());
        assertEquals("Penicilina", atualizado.getAlergias());
        verify(pacienteRepository).save(paciente);
    }

    @Test
    public void naoDeveAtualizarParaCpfDeOutroPaciente() {
        PacienteRequest atualizacao = new PacienteRequest(
                "Maria Souza",
                "99999999999",
                "Amil",
                null,
                null);

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByCpfAndIdNot("99999999999", 1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pacienteService.atualizar(1L, atualizacao));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    public void naoDeveAtualizarPacienteInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pacienteService.atualizar(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(pacienteRepository, never()).save(any());
    }


    @Test
    public void deveDeletarPacienteExistente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        pacienteService.deletar(1L);

        verify(pacienteRepository).delete(paciente);
    }

    @Test
    public void naoDeveDeletarPacienteInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pacienteService.deletar(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(pacienteRepository, never()).delete(any());
    }
}
