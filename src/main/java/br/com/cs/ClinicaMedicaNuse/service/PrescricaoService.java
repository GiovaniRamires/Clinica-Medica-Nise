package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.PrescricaoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Prescricao;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.PrescricaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PrescricaoService {

private final PrescricaoRepository prescricaoRepository;
private final ConsultaRepository consultaRepository;


public PrescricaoService(PrescricaoRepository prescricaoRepository, ConsultaRepository consultaRepository){
    this.prescricaoRepository = prescricaoRepository;
    this.consultaRepository = consultaRepository;
}

    @Transactional
    public Prescricao criar (PrescricaoRequest request){
    var consulta = consultaRepository.findById(request.consultaId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta id: " + request.consultaId() + " não encontrada"));

    var prescriacao = new Prescricao(
            consulta,
            request.medicamento(),
            request.dosagem(),
            request.frequencia(),
            request.duracao(),
            request.observacoes());
    return prescricaoRepository.save(prescriacao);
    }

    public Prescricao buscarPorId(Long id) {
        return prescricaoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Prescrição id: " + id + " não encontrada"));
    }

        public List<Prescricao> listarPorConsulta(Long consultaId){
        return prescricaoRepository.findByConsultaIdOrderByCriadoEmDesc(consultaId);
        }

        public List<Prescricao> listarPorPaciente(Long pacienteId){
        return prescricaoRepository.findByConsultaPacienteIdOrderByCriadoEmDesc(pacienteId);
        }

        @Transactional
        public Prescricao atualizar(Long id, PrescricaoRequest request){
        var prescricao = buscarPorId(id);
        prescricao.setMedicamento(request.medicamento());
        prescricao.setDosagem(request.dosagem());
        prescricao.setFrequencia(request.frequencia());
        prescricao.setDuracao(request.duracao());
        prescricao.setObservacoes(request.observacoes());
        return prescricaoRepository.save(prescricao);

        }

        public void deletar(Long id){
        var prescricao = buscarPorId(id);
        prescricaoRepository.delete(prescricao);
        }
}
