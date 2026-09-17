package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.repository.MedicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicoService {
    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public Medico criar(String nome, String crm, String especialidade) {
        Medico medico = new Medico(nome, crm, especialidade);
        return medicoRepository.save(medico);
    }

    public List<Medico> listarMedicos() {
        return medicoRepository.findAll();
    }

    public Medico buscarMedicoPorId(Long id) {
        return medicoRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Médico id: " + id + " não encontrado"));
    }

    public Medico atualizar(Long id, String nome, String crm, String especialidade) {
        var medico = buscarMedicoPorId(id);
        medico.setNome(nome);
        medico.setCrm(crm);
        medico.setEspecialidade(especialidade);
        return medicoRepository.save(medico);
    }

    public void deletar(Long id) {
        var medico = buscarMedicoPorId(id);
        medicoRepository.delete(medico);
    }
}
