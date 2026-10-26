package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.MedicoRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.MedicoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    public MedicoService(MedicoRepository medicoRepository, UsuarioRepository usuarioRepository) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Medico criar(MedicoRequest request) {
        // CRM identifica o médico: dois cadastros com o mesmo CRM são a mesma pessoa.
        if (medicoRepository.existsByCrm(request.crm())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Médico com CRM já cadastrado");
        }
        if (medicoRepository.existsByUsuarioId(request.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O usuário informado já está vinculado a outro médico");
        }

        Usuario usuario = buscarUsuarioMedico(request.usuarioId());

        var medico = new Medico(request.nome(), request.crm(), request.especialidade(), usuario);
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
        if (medicoRepository.existsByUsuarioIdAndIdNot(request.usuarioId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O usuário informado já está vinculado a outro médico");
        }

        Usuario usuario = buscarUsuarioMedico(request.usuarioId());

        medico.setNome(request.nome());
        medico.setCrm(request.crm());
        medico.setEspecialidade(request.especialidade());
        medico.setUsuario(usuario);
        return medicoRepository.save(medico);
    }

    public void deletar(Long id) {
        var medico = buscarMedicoPorId(id);
        medicoRepository.delete(medico);
    }

    /** O usuário ligado ao médico precisa existir e ter perfil MEDICO. */
    private Usuario buscarUsuarioMedico(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário id: " + usuarioId + " não encontrado"));

        if (usuario.getPerfil() != Usuario.Perfil.MEDICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O usuário informado não possui perfil MEDICO");
        }
        return usuario;
    }
}
