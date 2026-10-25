package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AlterarSenhaRequest;
import br.com.cs.ClinicaMedicaNuse.dto.CadastroPacienteRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginResponse;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioResponse;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.PacienteRepository;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import br.com.cs.ClinicaMedicaNuse.security.TokenService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
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

    /**
     * Auto-cadastro público do paciente: cria o Paciente (dado clínico vazio -
     * quem preenche isso é a clínica depois) e o Usuario (perfil PACIENTE) numa
     * única transação, já vinculados, e devolve o login pronto.
     */
    @Transactional
    public LoginResponse cadastrarPaciente(CadastroPacienteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail");
        }
        if (pacienteRepository.existsByCpf(request.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um paciente com esse CPF");
        }

        var usuario = usuarioRepository.save(new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                Usuario.Perfil.PACIENTE));

        var paciente = new Paciente(request.nome(), request.cpf(), request.convenio(), null, null);
        paciente.setUsuario(usuario);
        pacienteRepository.save(paciente);

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

    /** O registro de paciente ligado ao usuário logado (perfil PACIENTE). */
    public Paciente buscarPacienteLogado(Long usuarioId) {
        return pacienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum paciente vinculado a este usuário"));
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
