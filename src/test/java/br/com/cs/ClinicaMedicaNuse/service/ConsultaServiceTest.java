package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.ConsultaAtualizacaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ConsultaRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private ConsultaService consultaService;

    private Paciente paciente;
    private Medico medico;
    private Agendamento agendamento;
    private ConsultaRequest request;
    private Consulta consulta;

    @BeforeEach
    void setUp() {
        paciente = new Paciente("Maria", "12345678901", "Unimed", null, null);
        paciente.setId(1L);

        medico = new Medico("Dr. House", "123456", "Cardiologia");
        medico.setId(2L);

        agendamento = new Agendamento(paciente, medico,
                LocalDate.now(), LocalTime.of(10, 0), "obs");
        agendamento.setId(10L);

        request = new ConsultaRequest(10L, "Dor de cabeça", "Paciente estável",
                "Enxaqueca", "Repouso e hidratação");

        consulta = new Consulta(agendamento, "Dor de cabeça", "Paciente estável",
                "Enxaqueca", "Repouso e hidratação");
        consulta.setId(5L);
    }

    @Test
    public void deveCriarConsultaEMarcarAgendamentoComoRealizado() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

        Consulta criada = consultaService.criar(request);

        assertEquals("Dor de cabeça", criada.getQueixaPrincipal());
        assertEquals(paciente, criada.getPaciente());
        assertEquals(medico, criada.getMedico());
        assertEquals(SituacaoAgendamento.REALIZADO, agendamento.getSituacao());
        verify(consultaRepository).save(any(Consulta.class));
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    public void naoDeveCriarConsultaParaAgendamentoInexistente() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.criar(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    public void naoDeveCriarConsultaParaAgendamentoCancelado() {
        agendamento.setSituacao(SituacaoAgendamento.CANCELADO);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    public void naoDeveCriarConsultaParaPacienteAusente() {
        agendamento.setSituacao(SituacaoAgendamento.AUSENTE);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    public void naoDeveCriarConsultaQuandoAgendamentoJaRealizado() {
        agendamento.setSituacao(SituacaoAgendamento.REALIZADO);
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.criar(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    public void deveListarTodasAsConsultas() {
        when(consultaRepository.findAllByOrderByDataDescHorarioDesc()).thenReturn(List.of(consulta));

        List<Consulta> resultado = consultaService.listarConsultas();

        assertEquals(1, resultado.size());
    }

    @Test
    public void deveListarConsultasPorPaciente() {
        when(consultaRepository.findByPacienteIdOrderByDataDescHorarioDesc(1L))
                .thenReturn(List.of(consulta));

        List<Consulta> resultado = consultaService.listarPorPaciente(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    public void deveBuscarConsultaPorIdExistente() {
        when(consultaRepository.findById(5L)).thenReturn(Optional.of(consulta));

        Consulta encontrada = consultaService.buscarConsultaPorId(5L);

        assertEquals(5L, encontrada.getId());
    }

    @Test
    public void deveRetornar404AoBuscarConsultaInexistente() {
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.buscarConsultaPorId(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void deveAtualizarConsultaExistente() {
        ConsultaAtualizacaoRequest atualizacao = new ConsultaAtualizacaoRequest(
                "Dor no peito", "Encaminhado ao cardiologista", "Arritmia", "Exames complementares");

        when(consultaRepository.findById(5L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));

        Consulta atualizada = consultaService.atualizar(5L, atualizacao);

        assertEquals("Dor no peito", atualizada.getQueixaPrincipal());
        assertEquals("Arritmia", atualizada.getDiagnostico());
        verify(consultaRepository).save(consulta);
    }

    @Test
    public void naoDeveAtualizarConsultaInexistente() {
        ConsultaAtualizacaoRequest atualizacao = new ConsultaAtualizacaoRequest(
                "Dor no peito", null, null, null);
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.atualizar(99L, atualizacao));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    public void deveDeletarConsultaExistente() {
        when(consultaRepository.findById(5L)).thenReturn(Optional.of(consulta));

        consultaService.deletar(5L);

        verify(consultaRepository).delete(consulta);
    }

    @Test
    public void naoDeveDeletarConsultaInexistente() {
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> consultaService.deletar(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(consultaRepository, never()).delete(any());
    }
}
