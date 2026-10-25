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

    /**
     * @param comDadoClinico false quando quem cadastra nao pode gravar dado
     *                       clinico (recepcao): alergias e prontuario ficam vazios.
     */
    public Paciente criar(PacienteRequest request, boolean comDadoClinico) {
        if (pacienteRepository.existsByCpf(request.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente com CPF já cadastrado");
        }
        var paciente = new Paciente(
                request.nome(),
                request.cpf(),
                request.convenio(),
                comDadoClinico ? request.alergias() : null,
                comDadoClinico ? request.prontuario() : null);
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

    /**
     * @param comDadoClinico false quando quem edita nao pode mexer em dado
     *                       clinico: os valores ja gravados sao preservados, e nao apagados.
     */
    public Paciente atualizar(Long id, PacienteRequest request, boolean comDadoClinico) {
        var paciente = buscarPacientePorId(id);

        // O CPF só pode mudar para um que ainda não pertença a outro paciente.
        if (pacienteRepository.existsByCpfAndIdNot(request.cpf(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paciente com CPF já cadastrado");
        }

        paciente.setNome(request.nome());
        paciente.setCpf(request.cpf());
        paciente.setConvenio(request.convenio());
        if (comDadoClinico) {
            paciente.setAlergias(request.alergias());
            paciente.setProntuario(request.prontuario());
        }
        return pacienteRepository.save(paciente);
    }

    public void deletar(Long id) {
        var paciente = buscarPacientePorId(id);
        pacienteRepository.delete(paciente);
    }
}
