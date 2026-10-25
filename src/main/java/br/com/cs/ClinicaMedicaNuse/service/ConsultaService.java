package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.ConsultaAtualizacaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ConsultaRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final AgendamentoRepository agendamentoRepository;

    public ConsultaService(ConsultaRepository consultaRepository, AgendamentoRepository agendamentoRepository) {
        this.consultaRepository = consultaRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public Consulta criar(ConsultaRequest request) {
        var agendamento = agendamentoRepository.findById(request.agendamentoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Agendamento id: " + request.agendamentoId() + " não encontrado"));
        var situacao = agendamento.getSituacao();

        if (situacao == Agendamento.SituacaoAgendamento.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possivel ter uma consulta com um agendamento cancelado!");
        }
        if (situacao == Agendamento.SituacaoAgendamento.AUSENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente ausente, não é possivel registrar atendimento!");
        }
        if (situacao == Agendamento.SituacaoAgendamento.REALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agendamento já realizou uma consulta!");
        }
        Consulta consulta = new Consulta(
                agendamento,
                request.queixaPrincipal(),
                request.observacoesMedicas(),
                request.diagnostico(),
                request.conduta());
        consultaRepository.save(consulta);

        agendamento.setSituacao(Agendamento.SituacaoAgendamento.REALIZADO);
        agendamentoRepository.save(agendamento);
        return consulta;
    }


    public List<Consulta> listarConsultas() {
        return consultaRepository.findAllByOrderByDataDescHorarioDesc();
    }

    public List<Consulta> listarPorPaciente(Long pacienteId) {
        return consultaRepository.findByPacienteIdOrderByDataDescHorarioDesc(pacienteId);
    }

    public Consulta buscarConsultaPorId(Long id) {
        return consultaRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Consulta id: " + id + " não encontrada"));
    }

    public Consulta atualizar(Long id, ConsultaAtualizacaoRequest request) {
        var consulta = buscarConsultaPorId(id);
        consulta.setQueixaPrincipal(request.queixaPrincipal());
        consulta.setObservacoesMedicas(request.observacoesMedicas());
        consulta.setDiagnostico(request.diagnostico());
        consulta.setConduta(request.conduta());
        return consultaRepository.save(consulta);
    }

    public void deletar(Long id) {
        var consulta = buscarConsultaPorId(id);
        consultaRepository.delete(consulta);
    }
}
