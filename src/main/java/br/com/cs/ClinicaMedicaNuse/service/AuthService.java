package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AlterarSenhaRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginResponse;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioResponse;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import br.com.cs.ClinicaMedicaNuse.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        // Mesma mensagem para e-mail inexistente e senha errada, para nao revelar
        // quais e-mails estao cadastrados.
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(u -> u.getSenhaHash() != null && passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        return new LoginResponse(
                tokenService.gerar(usuario),
                tokenService.validadeEmSegundos(),
                UsuarioResponse.from(usuario));
    }

    /** Dados do usuário do token — o front usa para restaurar a sessão ao recarregar a página. */
    public Usuario buscarLogado(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário do token não existe mais"));
    }

    public void alterarSenha(Long usuarioId, AlterarSenhaRequest request) {
        var usuario = buscarLogado(usuarioId);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha atual incorreta");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A nova senha precisa ser diferente da atual");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
    }
}
