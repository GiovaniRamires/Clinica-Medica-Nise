package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.PacienteRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.repository.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente criar(PacienteRequest request) {
        if (pacienteRepository.existsByCpf(request.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente com CPF já cadastrado");
        }
        var paciente = new Paciente(
                request.nome(),
                request.cpf(),
                request.convenio(),
                request.alergias(),
                request.prontuario());
        return pacienteRepository.save(paciente);
    }

    public List<Paciente> listarPacientes() {
        return pacienteRepository.findAll();
    }

    public Paciente buscarPacientePorId(Long id) {
        return pacienteRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Paciente id: " + id + " não encontrado"));
    }

    public Paciente atualizar(Long id, PacienteRequest request) {
        var paciente = buscarPacientePorId(id);

        // O CPF só pode mudar para um que ainda não pertença a outro paciente.
        if (pacienteRepository.existsByCpfAndIdNot(request.cpf(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente com CPF já cadastrado");
        }

        paciente.setNome(request.nome());
        paciente.setCpf(request.cpf());
        paciente.setConvenio(request.convenio());
        paciente.setAlergias(request.alergias());
        paciente.setProntuario(request.prontuario());
        return pacienteRepository.save(paciente);
    }

    public void deletar(Long id) {
        var paciente = buscarPacientePorId(id);
        pacienteRepository.delete(paciente);
    }
}
