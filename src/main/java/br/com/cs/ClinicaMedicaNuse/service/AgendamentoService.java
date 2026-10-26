package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.HorarioDisponivelResponse;
import br.com.cs.ClinicaMedicaNuse.dto.SolicitacaoAgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;

@Service
public class AgendamentoService {

    private static final LocalTime ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    private static final int DURACAO_MINUTOS = 30;

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository, PacienteService pacienteService, MedicoService medicoService) {
        this.pacienteService = pacienteService;
        this.agendamentoRepository = agendamentoRepository;
        this.medicoService = medicoService;
    }

    public Agendamento criar(AgendamentoRequest request) {
        var paciente = pacienteService.buscarPacientePorId(request.pacienteId());
        var medico = medicoService.buscarMedicoPorId(request.medicoId());

        checarConflitoDeHorario(request.medicoId(), request.pacienteId(), request.data(), request.horario());

        var agendamento = new Agendamento(paciente, medico, request.data(), request.horario(), request.observacoes());
        return agendamentoRepository.save(agendamento);
    }

    public Agendamento solicitar(Paciente paciente, SolicitacaoAgendamentoRequest request) {
        var medico = medicoService.buscarMedicoPorId(request.medicoId());

        checarConflitoDeHorario(request.medicoId(), paciente.getId(), request.data(), request.horario());

        var agendamento = new Agendamento(paciente, medico, request.data(), request.horario(), request.observacoes());
        agendamento.setSituacao(SituacaoAgendamento.SOLICITADO);
        return agendamentoRepository.save(agendamento);
    }

    private void checarConflitoDeHorario(Long medicoId, Long pacienteId, LocalDate data, LocalTime horario) {
        validarHorarioNaGrade(horario);

        boolean horarioOcupado = agendamentoRepository.existsByMedicoIdAndDataAndHorarioAndSituacaoNot(
                medicoId, data, horario, SituacaoAgendamento.CANCELADO);
        if (horarioOcupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O médico já possui um agendamento nesse dia e horário");
        }

        boolean pacienteOcupado = agendamentoRepository.existsByPacienteIdAndDataAndHorarioAndSituacaoNot(
                pacienteId, data, horario, SituacaoAgendamento.CANCELADO);
        if (pacienteOcupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O paciente já possui um agendamento nesse dia e horário");
        }
    }

    private void validarHorarioNaGrade(LocalTime horario) {
        boolean dentroDoExpediente = !horario.isBefore(ABERTURA) && horario.isBefore(FECHAMENTO);
        boolean noPasso = horario.getMinute() % DURACAO_MINUTOS == 0 && horario.getSecond() == 0;
        if (!dentroDoExpediente || !noPasso) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Horário inválido. Atendimento das 08:00 às 18:00, em intervalos de 30 minutos.");
        }
    }

    public HorarioDisponivelResponse.Agenda horariosDoDia(Long medicoId, LocalDate data) {
        medicoService.buscarMedicoPorId(medicoId);

        Set<LocalTime> ocupados = new HashSet<>();
        for (Agendamento a : agendamentoRepository.findByMedicoIdAndDataAndSituacaoNot(medicoId, data, SituacaoAgendamento.CANCELADO)) {
            ocupados.add(a.getHorario());
        }

        List<HorarioDisponivelResponse> horarios = new ArrayList<>();
        int livres = 0;
        for (LocalTime t = ABERTURA; t.isBefore(FECHAMENTO); t = t.plusMinutes(DURACAO_MINUTOS)) {
            boolean livre = !ocupados.contains(t);
            if (livre) livres++;
            horarios.add(new HorarioDisponivelResponse(t, livre));
        }
        return new HorarioDisponivelResponse.Agenda(horarios.size(), livres, DURACAO_MINUTOS, horarios);
    }

    public List<Agendamento> listarAgendamentos() {
        return agendamentoRepository.findAllByOrderByDataAscHorarioAsc();
    }

    public List<Agendamento> listarPorPaciente(Long pacienteId) {
        return agendamentoRepository.findByPacienteIdOrderByDataAscHorarioAsc(pacienteId);
    }

    public Agendamento buscarAgendamentosPorId(Long id) {
        return agendamentoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento id: " + id + " não encontrado"));
    }

    public Agendamento marcarComoRealizado(Long id) {
        return alterarSituacao(id, SituacaoAgendamento.REALIZADO);
    }

    public Agendamento alterarSituacao(Long id, SituacaoAgendamento nova) {
        var agendamento = buscarAgendamentosPorId(id);
        var atual = agendamento.getSituacao();

        boolean permitido = switch (atual) {
            case SOLICITADO -> nova == SituacaoAgendamento.AGENDADO
                    || nova == SituacaoAgendamento.CANCELADO;
            case AGENDADO -> nova == SituacaoAgendamento.CONFIRMADO
                    || nova == SituacaoAgendamento.CANCELADO
                    || nova == SituacaoAgendamento.AUSENTE
                    || nova == SituacaoAgendamento.REALIZADO;
            case CONFIRMADO -> nova == SituacaoAgendamento.CANCELADO
                    || nova == SituacaoAgendamento.AUSENTE
                    || nova == SituacaoAgendamento.REALIZADO;
            default -> false;
        };

        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível mudar a situação de " + atual + " para " + nova);
        }

        agendamento.setSituacao(nova);
        return agendamentoRepository.save(agendamento);
    }
}