package br.com.cs.ClinicaMedicaNuse.security;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cria o primeiro ADMINISTRADOR quando o banco está vazio — sem ele não há como
 * fazer login nem cadastrar usuários (ovo e galinha).
 *
 * A senha vem de CLINICA_ADMIN_SENHA. Se a variável não estiver definida,
 * sorteamos uma senha forte e mostramos no log uma única vez: assim o projeto
 * roda sem configuração e sem senha fixa no código.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senhaConfigurada;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${clinica.admin.email:admin@clinicanise.local}") String email,
                                @Value("${clinica.admin.senha:}") String senhaConfigurada) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senhaConfigurada = senhaConfigurada;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        boolean sorteada = senhaConfigurada == null || senhaConfigurada.isBlank();
        String senha = sorteada ? sortearSenha() : senhaConfigurada;

        usuarioRepository.save(new Usuario(
                "Administrador",
                email,
                passwordEncoder.encode(senha),
                Usuario.Perfil.ADMINISTRADOR));

        if (sorteada) {
            log.warn("""
                    
                    ====================================================================
                     Primeiro acesso: administrador criado
                       e-mail: {}
                       senha : {}
                     Essa senha foi sorteada agora e nao sera mostrada de novo.
                     Troque em PUT /auth/senha, ou defina CLINICA_ADMIN_SENHA.
                    ====================================================================""", email, senha);
        } else {
            log.info("Administrador inicial criado com o e-mail {} e a senha de CLINICA_ADMIN_SENHA.", email);
        }
    }

    private static String sortearSenha() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
