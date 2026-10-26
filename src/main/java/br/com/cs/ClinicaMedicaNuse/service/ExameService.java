package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ExameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Exame.SituacaoExame;

@Service
public class ExameService {

    private final ExameRepository exameRepository;
    private final ConsultaRepository consultaRepository;

    public ExameService(ExameRepository exameRepository, ConsultaRepository consultaRepository) {
        this.exameRepository = exameRepository;
        this.consultaRepository = consultaRepository;
    }

    public Exame solicitar(Long consultaId, String nome) {
        var consulta = consultaRepository.findById(consultaId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Consulta id: " + consultaId + " não encontrada"));
        var exame = new Exame(consulta, nome);
        return exameRepository.save(exame);
    }

    public List<Exame> listar() {
        return exameRepository.findAllByOrderByDataSolicitacaoDesc();
    }

    public List<Exame> listarPorPaciente(Long pacienteId) {
        return exameRepository.findByPacienteIdOrderByDataSolicitacaoDesc(pacienteId);
    }

    public List<Exame> listarPendentes() {
        // Pendente = tudo que ainda não foi realizado nem cancelado.
        return exameRepository.findBySituacaoNotOrderByDataSolicitacaoDesc(SituacaoExame.REALIZADO)
                .stream()
                .filter(e -> e.getSituacao() != SituacaoExame.CANCELADO)
                .toList();
    }

    public Exame buscarPorId(Long id) {
        return exameRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Exame id: " + id + " não encontrado"));
    }

    public Exame alterarSituacao(Long id, SituacaoExame nova) {
        var exame = buscarPorId(id);
        var atual = exame.getSituacao();

        boolean permitido = switch (atual) {
            case SOLICITADO -> nova == SituacaoExame.AGENDADO
                    || nova == SituacaoExame.REALIZADO
                    || nova == SituacaoExame.CANCELADO;
            case AGENDADO -> nova == SituacaoExame.REALIZADO
                    || nova == SituacaoExame.CANCELADO;
            default -> false;
        };
        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível mudar a situação de " + atual + " para " + nova);
        }

        exame.setSituacao(nova);
        if (nova == SituacaoExame.REALIZADO && exame.getDataRealizacao() == null) {
            exame.setDataRealizacao(LocalDate.now());
        }
        return exameRepository.save(exame);
    }

    public Exame registrarResultado(Long id, String resultado) {
        var exame = buscarPorId(id);
        exame.setResultado(resultado);
        if (exame.getSituacao() != SituacaoExame.REALIZADO) {
            exame.setSituacao(SituacaoExame.REALIZADO);
        }
        if (exame.getDataRealizacao() == null) {
            exame.setDataRealizacao(LocalDate.now());
        }
        return exameRepository.save(exame);
    }
}
