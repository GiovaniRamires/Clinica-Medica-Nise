package br.com.cs.ClinicaMedicaNuse.service;


import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.SolicitacaoAgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;

@Service
public class AgendamentoService {

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

    /**
     * Solicitação feita pelo próprio paciente logado: vira um agendamento em
     * SOLICITADO, que a recepção/administrador confirma (vira AGENDADO) ou
     * recusa (vira CANCELADO). O paciente nunca cria um agendamento já
     * confirmado sem passar por essa triagem.
     */
    public Agendamento solicitar(Paciente paciente, SolicitacaoAgendamentoRequest request) {
        var medico = medicoService.buscarMedicoPorId(request.medicoId());

        checarConflitoDeHorario(request.medicoId(), paciente.getId(), request.data(), request.horario());

        var agendamento = new Agendamento(paciente, medico, request.data(), request.horario(), request.observacoes());
        agendamento.setSituacao(SituacaoAgendamento.SOLICITADO);
        return agendamentoRepository.save(agendamento);
    }

    private void checarConflitoDeHorario(Long medicoId, Long pacienteId, LocalDate data, LocalTime horario) {
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

    public List<Agendamento> listarAgendamentos() {
        return agendamentoRepository.findAllByOrderByDataAscHorarioAsc();
    }

    /** Agenda do próprio paciente logado. */
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
            // A recepção/administrador confirma a solicitação do paciente (vira
            // um agendamento normal) ou recusa.
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
