package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private PacienteService pacienteService;
    @Mock
    private MedicoService medicoService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Paciente paciente;
    private Medico medico;
    private LocalDate data;
    private LocalTime horario;
    private AgendamentoRequest request;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        paciente = new Paciente("Maria", "12345678901", "Unimed", null, null);
        paciente.setId(1L);

        medico = new Medico("Dr. House", "123456", "Cardiologia",
                new Usuario("Dr. House", "house@clinicanise.local", "hash", Usuario.Perfil.MEDICO));
        medico.setId(2L);

        data = LocalDate.now().plusDays(1);
        horario = LocalTime.of(10, 0);

        request = new AgendamentoRequest(1L, 2L, data, horario, "Primeira consulta");

        agendamento = new Agendamento(paciente, medico, data, horario, "Primeira consulta");
        agendamento.setId(10L);
    }

    @Test
    public void deveCriarAgendamentoQuandoHorarioLivre() {
        when(pacienteService.buscarPacientePorId(1L)).thenReturn(paciente);
        when(medicoService.buscarMedicoPorId(2L)).thenReturn(medico);
        when(agendamentoRepository.existsByMedicoIdAndDataAndHorarioAndSituacaoNotIn(
                eq(2L), eq(data), eq(horario), eq(List.of(SituacaoAgendamento.CANCELADO, SituacaoAgendamento.AUSENTE)))).thenReturn(false);
        when(agendamentoRepository.existsByPacienteIdAndDataAndHorarioAndSituacaoNotIn(
                eq(1L), eq(data), eq(horario), eq(List.of(SituacaoAgendamento.CANCELADO, SituacaoAgendamento.AUSENTE)))).thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento criado = agendamentoService.criar(request);

        assertEquals(paciente, criado.getPaciente());
        assertEquals(medico, criado.getMedico());
        assertEquals(SituacaoAgendamento.AGENDADO, criado.getSituacao());
        verify(agendamentoRepository).save(any(Agendamento.class));
    }

    @Test
    public void naoDeveCriarQuandoMedicoJaOcupado() {
        when(pacienteService.buscarPacientePorId(1L)).thenReturn(paciente);
        when(medicoService.buscarMedicoPorId(2L)).thenReturn(medico);
        when(agendamentoRepository.existsByMedicoIdAndDataAndHorarioAndSituacaoNotIn(
                eq(2L), eq(data), eq(horario), eq(List.of(SituacaoAgendamento.CANCELADO, SituacaoAgendamento.AUSENTE)))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    public void naoDeveCriarQuandoPacienteJaOcupado() {
        when(pacienteService.buscarPacientePorId(1L)).thenReturn(paciente);
        when(medicoService.buscarMedicoPorId(2L)).thenReturn(medico);
        when(agendamentoRepository.existsByMedicoIdAndDataAndHorarioAndSituacaoNotIn(
                eq(2L), eq(data), eq(horario), eq(List.of(SituacaoAgendamento.CANCELADO, SituacaoAgendamento.AUSENTE)))).thenReturn(false);
        when(agendamentoRepository.existsByPacienteIdAndDataAndHorarioAndSituacaoNotIn(
                eq(1L), eq(data), eq(horario), eq(List.of(SituacaoAgendamento.CANCELADO, SituacaoAgendamento.AUSENTE)))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    public void deveListarAgendamentosEmOrdemCronologica() {
        when(agendamentoRepository.findAllByOrderByDataAscHorarioAsc()).thenReturn(List.of(agendamento));

        List<Agendamento> resultado = agendamentoService.listarAgendamentos();

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).getId());
    }

    @Test
    public void deveBuscarAgendamentoPorIdExistente() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        Agendamento encontrado = agendamentoService.buscarAgendamentosPorId(10L);

        assertEquals(10L, encontrado.getId());
    }

    @Test
    public void deveRetornar404AoBuscarAgendamentoInexistente() {
        when(agendamentoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.buscarAgendamentosPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void deveConfirmarAgendamentoAgendado() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento resultado = agendamentoService.alterarSituacao(10L, SituacaoAgendamento.CONFIRMADO);

        assertEquals(SituacaoAgendamento.CONFIRMADO, resultado.getSituacao());
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    public void deveMarcarComoRealizado() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento resultado = agendamentoService.marcarComoRealizado(10L);

        assertEquals(SituacaoAgendamento.REALIZADO, resultado.getSituacao());
    }

    @Test
    public void naoDeveAlterarSituacaoDeAgendamentoCancelado() {
        agendamento.setSituacao(SituacaoAgendamento.CANCELADO);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.alterarSituacao(10L, SituacaoAgendamento.CONFIRMADO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    public void pacienteCancelaPropriaConsultaEHorarioFicaLivre() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Agendamento resultado = agendamentoService.cancelarDoPaciente(paciente, 10L);

        assertEquals(SituacaoAgendamento.CANCELADO, resultado.getSituacao());
    }

    @Test
    public void pacienteNaoCancelaConsultaDeOutroPaciente() {
        var outro = new Paciente("Ana", "98765432100", "Unimed", null, null);
        outro.setId(99L);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.cancelarDoPaciente(outro, 10L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals(SituacaoAgendamento.AGENDADO, agendamento.getSituacao());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    public void pacienteNaoCancelaConsultaQueJaPassou() {
        var passada = new Agendamento(paciente, medico, LocalDate.now().minusDays(1), horario, "x");
        passada.setId(11L);
        when(agendamentoRepository.findById(11L)).thenReturn(Optional.of(passada));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.cancelarDoPaciente(paciente, 11L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    public void pacienteNaoCancelaConsultaJaRealizada() {
        agendamento.setSituacao(SituacaoAgendamento.REALIZADO);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> agendamentoService.cancelarDoPaciente(paciente, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
