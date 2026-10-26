package br.com.cs.ClinicaMedicaNuse.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())   // reaproveita o CorsConfig e libera o preflight OPTIONS
                .csrf(csrf -> csrf.disable())      // API stateless com Bearer token, sem cookie de sessão
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/logout", "/auth/cadastro-paciente", "/error").permitAll()
                        // Interface web (pasta front-end/ publicada em /app/) e a raiz que redireciona para ela
                        .requestMatchers("/", "/app/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // O paciente só acessa a própria agenda pelas rotas dedicadas em /auth
                        .requestMatchers("/auth/minhas-consultas", "/auth/solicitar-agendamento").hasRole("PACIENTE")

                        // Dado clínico: só quem atende. A recepção não lê prontuário nem consulta.
                        .requestMatchers("/consultas/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/pacientes/*/prontuario").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/pacientes/*/consultas").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/exames/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/prescricoes/**").hasAnyRole("MEDICO", "ADMINISTRADOR")

                        // Cadastro de usuários do sistema é só do administrador
                        .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")

                        // Excluir paciente apaga histórico: só administrador
                        .requestMatchers(HttpMethod.DELETE, "/pacientes/**").hasRole("ADMINISTRADOR")
                        // Cadastrar/editar paciente e cuidar da agenda: equipe da clínica, nao o
                        // proprio paciente (ele usa as rotas dedicadas acima)
                        .requestMatchers("/pacientes/**", "/agendamentos/**").hasAnyRole("MEDICO", "RECEPCIONISTA", "ADMINISTRADOR")

                        // Corpo clínico: leitura para a equipe da clínica e para o paciente (que
                        // precisa da lista para escolher médico ao solicitar consulta); manutenção
                        // só do administrador.
                        .requestMatchers(HttpMethod.GET, "/medicos/**").hasAnyRole("MEDICO", "RECEPCIONISTA", "ADMINISTRADOR", "PACIENTE")
                        .requestMatchers("/medicos/**").hasRole("ADMINISTRADOR")

                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        // Sem isso o Security devolve 401/403 com corpo vazio, e o front
                        // nao tem mensagem para mostrar.
                        .authenticationEntryPoint((req, res, falha) -> escreverErro(res, req, 401,
                                "Unauthorized", "Faça login para acessar este recurso"))
                        .accessDeniedHandler((req, res, falha) -> escreverErro(res, req, 403,
                                "Forbidden", "Seu perfil não tem permissão para esta operação")))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    /** Escreve o mesmo formato do ErroResponse usado pelo ApiExceptionHandler. */
    private static void escreverErro(HttpServletResponse res, HttpServletRequest req,
                                     int status, String erro, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("""
                {"status":%d,"erro":"%s","mensagem":"%s","caminho":"%s","momento":"%s","campos":[]}"""
                .formatted(status, erro, mensagem, req.getRequestURI(), LocalDateTime.now()));
    }

    /**
     * Converte a claim "perfil" do token em authority ROLE_<PERFIL>.
     * O "name" da autenticação passa a ser o subject do token, que é o id do usuário.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("perfil");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Chave de assinatura do JWT. Vem de CLINICA_JWT_SECRET; se não estiver
     * definida, geramos uma aleatória nesta execução — assim o projeto roda sem
     * configuração e sem segredo fixo no repositório. O efeito de não definir a
     * variável é que os tokens param de valer quando a aplicação reinicia.
     */
    @Bean
    SecretKey jwtSecretKey(@Value("${clinica.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            log.warn("CLINICA_JWT_SECRET nao definida: usando uma chave aleatoria desta execucao. " +
                    "Os tokens emitidos deixam de valer quando a aplicacao reiniciar.");
            return new SecretKeySpec(Base64.getEncoder().encode(aleatorio), "HmacSHA256");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("clinica.jwt.secret precisa ter no minimo 32 caracteres para HS256");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
