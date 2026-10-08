package br.com.cs.ClinicaMedicaNuse.service;


import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
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

    public Agendamento criar(Long pacienteId, Long medicoId, LocalDate data, LocalTime horario, String observacoes) {
        var paciente = pacienteService.buscarPacientePorId(pacienteId);
        var medico = medicoService.buscarMedicoPorId(medicoId);

        boolean horarioOcupado = agendamentoRepository.existsMedicoIdAndDataAndHorarioAndSituacaoNot(medicoId, data, horario, SituacaoAgendamento.CANCELADO);
        if (horarioOcupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O médico já possui um agendamento nesse dia e horário");
        }

        var agendamento = new Agendamento(paciente, medico, data, horario, observacoes);

        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> listarAgendamentos(){
        return agendamentoRepository.findAll();
    }

    public Agendamento buscarAgendamentosPorId(Long id){
        return agendamentoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "ID não encontrado"));
    }

    public Agendamento marcarComoRealizado(Agendamento agendamento){
        agendamento.setSituacao((SituacaoAgendamento.REALIZADO));
        return agendamentoRepository.save(agendamento);
    }

}