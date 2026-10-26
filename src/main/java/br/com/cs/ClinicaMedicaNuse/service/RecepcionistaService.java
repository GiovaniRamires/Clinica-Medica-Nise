package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.RecepcionistaRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Recepcionista;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.RecepcionistaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RecepcionistaService {

    private final RecepcionistaRepository recepcionistaRepository;
    private final UsuarioRepository usuarioRepository;

    public RecepcionistaService(RecepcionistaRepository recepcionistaRepository,
                                UsuarioRepository usuarioRepository) {
        this.recepcionistaRepository = recepcionistaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Recepcionista criar(RecepcionistaRequest request) {
        if (recepcionistaRepository.existsByMatricula(request.matricula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Recepcionista com matrícula já cadastrada");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário id: " + request.usuarioId() + " não encontrado"));

        if (usuario.getPerfil() != Usuario.Perfil.RECEPCIONISTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O usuário informado não possui perfil RECEPCIONISTA");
        }

        var recepcionista = new Recepcionista(
                request.nome(), request.matricula(), request.turno(), usuario);
        return recepcionistaRepository.save(recepcionista);
    }

    public List<Recepcionista> listarTodas() {
        return recepcionistaRepository.findAll();
    }

    public Recepcionista buscarPorId(Long id) {
        return recepcionistaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Recepcionista id: " + id + " não encontrada"));
    }

    public Recepcionista atualizar(Long id, RecepcionistaRequest request) {
        var recepcionista = buscarPorId(id);

        if (recepcionistaRepository.existsByMatriculaAndIdNot(request.matricula(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Recepcionista com matrícula já cadastrada");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário id: " + request.usuarioId() + " não encontrado"));

        if (usuario.getPerfil() != Usuario.Perfil.RECEPCIONISTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O usuário informado não possui perfil RECEPCIONISTA");
        }

        recepcionista.setNome(request.nome());
        recepcionista.setMatricula(request.matricula());
        recepcionista.setTurno(request.turno());
        recepcionista.setUsuario(usuario);
        return recepcionistaRepository.save(recepcionista);
    }

    public void deletar(Long id) {
        var recepcionista = buscarPorId(id);
        recepcionistaRepository.delete(recepcionista);
    }
}
