package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PacienteService {
    private final PacienteRepository pacienteRepository;
    public PacienteService (PacienteRepository pacienteRepository){
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente criar (String nome, String cpf, String convenio, String alergias, String prontuario){
        Paciente paciente = new Paciente(nome, cpf, convenio, alergias, prontuario);
        pacienteRepository.save(paciente);
        return paciente;
    }

    public List<Paciente> listarPacientes (){
        return pacienteRepository.findAll();
    }

    public Paciente buscarPacientePorId(Long id){
        return pacienteRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Médico id: " + id + " não encontrado"));
    }

    public Paciente atualizar (Long id, String nome, String cpf, String convenio, String alergias, String prontuario){
        var paciente = buscarPacientePorId(id);
        paciente.setNome(nome);
        paciente.setCpf(cpf);
        paciente.setConvenio(convenio);
        paciente.setAlergias(alergias);
        paciente.setProntuario(prontuario);
        return pacienteRepository.save(paciente);
    }

    public void deletar(Long id){
        var paciente = buscarPacientePorId(id);
        pacienteRepository.delete(paciente);
    }
}
