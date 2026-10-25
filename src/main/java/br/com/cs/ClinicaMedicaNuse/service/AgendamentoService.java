package br.com.cs.ClinicaMedicaNuse.service;


import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

        boolean horarioOcupado = agendamentoRepository.existsByMedicoIdAndDataAndHorarioAndSituacaoNot(
                request.medicoId(), request.data(), request.horario(), SituacaoAgendamento.CANCELADO);
        if (horarioOcupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O médico já possui um agendamento nesse dia e horário");
        }

        boolean pacienteOcupado = agendamentoRepository.existsByPacienteIdAndDataAndHorarioAndSituacaoNot(
                request.pacienteId(), request.data(), request.horario(), SituacaoAgendamento.CANCELADO);
        if (pacienteOcupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O paciente já possui um agendamento nesse dia e horário");
        }

        var agendamento = new Agendamento(paciente, medico, request.data(), request.horario(), request.observacoes());

        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> listarAgendamentos() {
        return agendamentoRepository.findAllByOrderByDataAscHorarioAsc();
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
