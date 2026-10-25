package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.MedicoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.repository.MedicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public Medico criar(MedicoRequest request) {
        // CRM identifica o médico: dois cadastros com o mesmo CRM são a mesma pessoa.
        if (medicoRepository.existsByCrm(request.crm())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Médico com CRM já cadastrado");
        }
        var medico = new Medico(request.nome(), request.crm(), request.especialidade());
        return medicoRepository.save(medico);
    }

    public List<Medico> listarMedicos() {
        return medicoRepository.findAll();
    }

    public Medico buscarMedicoPorId(Long id) {
        return medicoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Médico id: " + id + " não encontrado"));
    }

    public Medico atualizar(Long id, MedicoRequest request) {
        var medico = buscarMedicoPorId(id);

        if (medicoRepository.existsByCrmAndIdNot(request.crm(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Médico com CRM já cadastrado");
        }

        medico.setNome(request.nome());
        medico.setCrm(request.crm());
        medico.setEspecialidade(request.especialidade());
        return medicoRepository.save(medico);
    }

    public void deletar(Long id) {
        var medico = buscarMedicoPorId(id);
        medicoRepository.delete(medico);
    }
}
