package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.UsuarioAtualizacaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioRequest;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        var usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),   // só o hash é gravado
                request.perfil());
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    public Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuário id: " + id + " não encontrado"));
    }

    public Usuario atualizar(Long id, UsuarioAtualizacaoRequest request) {
        var usuario = buscarUsuarioPorId(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        // Rebaixar o último administrador deixaria o sistema sem quem cadastra usuário.
        if (usuario.getPerfil() == Usuario.Perfil.ADMINISTRADOR
                && request.perfil() != Usuario.Perfil.ADMINISTRADOR
                && usuarioRepository.countByPerfil(Usuario.Perfil.ADMINISTRADOR) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este é o último administrador: cadastre outro antes de mudar o perfil deste");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPerfil(request.perfil());
        return usuarioRepository.save(usuario);
    }

    public void deletar(Long id, Long idDoLogado) {
        var usuario = buscarUsuarioPorId(id);

        if (id.equals(idDoLogado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode excluir o seu próprio usuário");
        }
        if (usuario.getPerfil() == Usuario.Perfil.ADMINISTRADOR
                && usuarioRepository.countByPerfil(Usuario.Perfil.ADMINISTRADOR) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este é o último administrador: o sistema ficaria sem ninguém para cadastrar usuários");
        }

        usuarioRepository.delete(usuario);
    }
}
