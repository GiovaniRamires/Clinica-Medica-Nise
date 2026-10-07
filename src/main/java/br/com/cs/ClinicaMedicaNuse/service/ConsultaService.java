package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
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

    public Consulta criar(Long agendamentoId, String queixaPrincipal, String observacoesMedicas, String diagnostico, String conduta) {
        var agendamento = agendamentoRepository.findById(agendamentoId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Agendamento id: " + agendamentoId + " não encontrado"));
        Consulta consulta = new Consulta(agendamento, queixaPrincipal, observacoesMedicas, diagnostico, conduta);
        return consultaRepository.save(consulta);
    }

    public List<Consulta> listarConsultas() {
        return consultaRepository.findAll();
    }

    public Consulta buscarConsultaPorId(Long id) {
        return consultaRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Consulta id: " + id + " não encontrada"));
    }

    public Consulta atualizar(Long id, String queixaPrincipal, String observacoesMedicas, String diagnostico, String conduta) {
        var consulta = buscarConsultaPorId(id);
        consulta.setQueixaPrincipal(queixaPrincipal);
        consulta.setObservacoesMedicas(observacoesMedicas);
        consulta.setDiagnostico(diagnostico);
        consulta.setConduta(conduta);
        return consultaRepository.save(consulta);
    }

    public void deletar(Long id) {
        var consulta = buscarConsultaPorId(id);
        consultaRepository.delete(consulta);
    }
}