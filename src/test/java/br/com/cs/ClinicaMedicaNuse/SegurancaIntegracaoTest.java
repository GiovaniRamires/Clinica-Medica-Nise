package br.com.cs.ClinicaMedicaNuse;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do login e das permissoes. Nao usa usuario simulado: cria usuarios de
 * verdade com hash BCrypt, faz POST /auth/login e usa o JWT devolvido no header
 * Authorization — ou seja, exercita a cadeia inteira (BCrypt -> token -> regra
 * de acesso por perfil).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SegurancaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private static final String SENHA = "clinica2026";

    @BeforeEach
    void limparUsuarios() {
        // O AdministradorInicial cria um admin no boot; aqui queremos um estado
        // conhecido. O @Transactional desfaz isso no fim de cada teste.
        usuarioRepository.deleteAll();
    }

    /* --------------------------- utilidades --------------------------- */

    private Usuario criarUsuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(
                "Usuario " + perfil, email, passwordEncoder.encode(SENHA), perfil));
    }

    private String corpoLogin(String email, String senha, int statusEsperado) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, senha)))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
    }

    /** Cria o usuario, faz login de verdade e devolve o token. */
    private String tokenDe(Usuario.Perfil perfil) throws Exception {
        String email = perfil.name().toLowerCase() + "@clinicanise.local";
        criarUsuario(email, perfil);
        return JsonPath.read(corpoLogin(email, SENHA, 200), "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /* ------------------------------ login ----------------------------- */

    @Test
    @DisplayName("Login correto devolve token e os dados do usuario, sem senha nem hash")
    void loginDevolveTokenSemExporSenha() throws Exception {
        criarUsuario("recepcao@clinicanise.local", Usuario.Perfil.RECEPCIONISTA);

        String corpo = corpoLogin("recepcao@clinicanise.local", SENHA, 200);

        assertThat((String) JsonPath.read(corpo, "$.token")).isNotBlank();
        assertThat(((Number) JsonPath.read(corpo, "$.expiraEmSegundos")).longValue()).isPositive();
        assertThat((String) JsonPath.read(corpo, "$.usuario.email")).isEqualTo("recepcao@clinicanise.local");
        assertThat((String) JsonPath.read(corpo, "$.usuario.perfil")).isEqualTo("RECEPCIONISTA");
        assertThat(((Map<String, Object>) JsonPath.read(corpo, "$.usuario")).keySet())
                .containsExactlyInAnyOrder("id", "nome", "email", "perfil");
        assertThat(corpo).doesNotContain("senha").doesNotContain("$2a$").doesNotContain("$2b$");
    }

    @Test
    @DisplayName("Senha errada e e-mail inexistente devolvem 401 com a mesma mensagem")
    void naoRevelaQuaisEmailsExistem() throws Exception {
        criarUsuario("recepcao@clinicanise.local", Usuario.Perfil.RECEPCIONISTA);

        String senhaErrada = corpoLogin("recepcao@clinicanise.local", "errada123", 401);
        String emailInexistente = corpoLogin("ninguem@clinicanise.local", SENHA, 401);

        assertThat((String) JsonPath.read(senhaErrada, "$.mensagem"))
                .isEqualTo(JsonPath.read(emailInexistente, "$.mensagem"));
    }

    @Test
    @DisplayName("A senha e gravada como hash BCrypt, nunca em texto")
    void senhaEGravadaComoHash() throws Exception {
        String token = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dra. Erika","email":"erika@clinicanise.local",
                                 "senha":"outraSenha123","perfil":"MEDICO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        var gravado = usuarioRepository.findByEmail("erika@clinicanise.local").orElseThrow();
        assertThat(gravado.getSenhaHash()).isNotEqualTo("outraSenha123").startsWith("$2");
        assertThat(passwordEncoder.matches("outraSenha123", gravado.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("O token e um JWT com subject = id do usuario e claim perfil")
    void tokenCarregaIdEPerfil() throws Exception {
        var usuario = criarUsuario("medico@clinicanise.local", Usuario.Perfil.MEDICO);
        String token = JsonPath.read(corpoLogin("medico@clinicanise.local", SENHA, 200), "$.token");

        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(usuario.getId()));
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("MEDICO");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("clinica-nise");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("GET /auth/eu devolve o usuario do token")
    void authEuDevolveUsuarioDoToken() throws Exception {
        String token = tokenDe(Usuario.Perfil.MEDICO);

        mockMvc.perform(get("/auth/eu").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("medico@clinicanise.local"))
                .andExpect(jsonPath("$.perfil").value("MEDICO"));
    }

    /* ---------------------------- sem token --------------------------- */

    @Test
    @DisplayName("Sem token a API devolve 401 no formato ErroResponse")
    void semTokenDevolve401() throws Exception {
        mockMvc.perform(get("/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("Token invalido devolve 401")
    void tokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get("/pacientes").header("Authorization", "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login e a interface web continuam publicos")
    void rotasPublicasSeguemAbertas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ninguem@clinicanise.local","senha":"qualquer"}
                                """))
                .andExpect(status().isUnauthorized());   // 401 do login, nao 403 de rota fechada

        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection());
    }

    /* ------------------------- permissoes por perfil ------------------ */

    @Test
    @DisplayName("Recepcionista cuida de pacientes e agenda")
    void recepcionistaUsaPacientesEAgenda() throws Exception {
        String token = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        mockMvc.perform(get("/pacientes").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/agendamentos").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Recepcionista nao acessa dado clinico")
    void recepcionistaNaoVeDadoClinico() throws Exception {
        String token = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        mockMvc.perform(get("/consultas").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/pacientes/1/prontuario").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/pacientes/1/consultas").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Medico acessa consultas e prontuario")
    void medicoAcessaDadoClinico() throws Exception {
        String token = tokenDe(Usuario.Perfil.MEDICO);

        mockMvc.perform(get("/consultas").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        // Cria paciente e consulta para o prontuario ter conteudo
        String paciente = mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344"}
                                """))
                .andReturn().getResponse().getContentAsString();
        long pacienteId = ((Number) JsonPath.read(paciente, "$.id")).longValue();

        mockMvc.perform(get("/pacientes/{id}/prontuario", pacienteId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConsultas").value(0));
    }

    @Test
    @DisplayName("Somente administrador cadastra usuario")
    void somenteAdminCadastraUsuario() throws Exception {
        String medico = tokenDe(Usuario.Perfil.MEDICO);

        mockMvc.perform(get("/usuarios").header("Authorization", bearer(medico)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(medico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Novo","email":"novo@clinicanise.local",
                                 "senha":"senhaboa123","perfil":"RECEPCIONISTA"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Somente administrador cadastra medico e exclui paciente")
    void somenteAdminMantemCorpoClinico() throws Exception {
        String recepcao = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        mockMvc.perform(post("/medicos")
                        .header("Authorization", bearer(recepcao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Cardiologia"}
                                """))
                .andExpect(status().isForbidden());

        // mas pode consultar o corpo clinico para montar a agenda
        mockMvc.perform(get("/medicos").header("Authorization", bearer(recepcao)))
                .andExpect(status().isOk());

        String paciente = mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(recepcao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344"}
                                """))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(delete("/pacientes/{id}", ((Number) JsonPath.read(paciente, "$.id")).longValue())
                        .header("Authorization", bearer(recepcao)))
                .andExpect(status().isForbidden());
    }

    /* --------------------------- troca de senha ----------------------- */

    @Test
    @DisplayName("Usuario troca a propria senha e passa a logar com a nova")
    void trocaDeSenhaFunciona() throws Exception {
        criarUsuario("recepcao@clinicanise.local", Usuario.Perfil.RECEPCIONISTA);
        String token = JsonPath.read(corpoLogin("recepcao@clinicanise.local", SENHA, 200), "$.token");

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual":"%s","novaSenha":"senhaNova2026"}
                                """.formatted(SENHA)))
                .andExpect(status().isNoContent());

        corpoLogin("recepcao@clinicanise.local", "senhaNova2026", 200);
        corpoLogin("recepcao@clinicanise.local", SENHA, 401);
    }

    @Test
    @DisplayName("Trocar senha exige a senha atual correta")
    void trocaDeSenhaExigeSenhaAtual() throws Exception {
        String token = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        mockMvc.perform(put("/auth/senha")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senhaAtual":"naoEhEssa","novaSenha":"senhaNova2026"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Senha curta cai na validacao do DTO")
    void senhaCurtaEhRecusada() throws Exception {
        String token = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Novo","email":"novo@clinicanise.local",
                                 "senha":"123","perfil":"RECEPCIONISTA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    /* ------------------------ cadastro de usuarios -------------------- */

    @Test
    @DisplayName("E-mail repetido devolve 409")
    void emailDuplicadoDevolve409() throws Exception {
        String token = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        mockMvc.perform(post("/usuarios")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Admin","email":"administrador@clinicanise.local",
                                 "senha":"senhaboa123","perfil":"ADMINISTRADOR"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Administrador nao exclui o proprio usuario")
    void adminNaoExcluiSiMesmo() throws Exception {
        var admin = criarUsuario("administrador@clinicanise.local", Usuario.Perfil.ADMINISTRADOR);
        String token = JsonPath.read(corpoLogin("administrador@clinicanise.local", SENHA, 200), "$.token");

        mockMvc.perform(delete("/usuarios/{id}", admin.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("O sistema nao fica sem administrador")
    void naoDeixaOSistemaSemAdministrador() throws Exception {
        var admin = criarUsuario("administrador@clinicanise.local", Usuario.Perfil.ADMINISTRADOR);
        var outroAdmin = criarUsuario("admin2@clinicanise.local", Usuario.Perfil.ADMINISTRADOR);
        String token = JsonPath.read(corpoLogin("administrador@clinicanise.local", SENHA, 200), "$.token");

        // Com dois administradores, excluir um deles e permitido
        mockMvc.perform(delete("/usuarios/{id}", outroAdmin.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        // Agora sobrou um: rebaixar o perfil dele e recusado
        mockMvc.perform(put("/usuarios/{id}", admin.getId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Administrador","email":"administrador@clinicanise.local",
                                 "perfil":"RECEPCIONISTA"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Recepcionista nao recebe alergias nem prontuario na lista de pacientes")
    void recepcionistaNaoRecebeDadoClinicoNaListagem() throws Exception {
        String admin = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        // O administrador cadastra o paciente COM dado clinico
        String criado = mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344","convenio":"Unimed",
                                 "alergias":"Dipirona","prontuario":"Hipertensa"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alergias").value("Dipirona"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(criado, "$.id")).longValue();

        String recepcao = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        // Para a recepcao os mesmos campos vem vazios, na listagem e no detalhe
        mockMvc.perform(get("/pacientes").header("Authorization", bearer(recepcao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria Silva"))
                .andExpect(jsonPath("$[0].convenio").value("Unimed"))
                .andExpect(jsonPath("$[0].alergias").doesNotExist())
                .andExpect(jsonPath("$[0].prontuario").doesNotExist());

        mockMvc.perform(get("/pacientes/{id}", id).header("Authorization", bearer(recepcao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias").doesNotExist());
    }

    @Test
    @DisplayName("Recepcionista editando paciente nao apaga o dado clinico ja gravado")
    void recepcionistaNaoApagaDadoClinico() throws Exception {
        String admin = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        String criado = mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344",
                                 "alergias":"Dipirona","prontuario":"Hipertensa"}
                                """))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(criado, "$.id")).longValue();

        String recepcao = tokenDe(Usuario.Perfil.RECEPCIONISTA);

        // A recepcao corrige o nome; os campos clinicos vao nulos no corpo dela
        mockMvc.perform(put("/pacientes/{id}", id)
                        .header("Authorization", bearer(recepcao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva Souza","cpf":"11122233344","convenio":"Amil"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva Souza"));

        // Visto pelo administrador, alergia e prontuario continuam lá
        mockMvc.perform(get("/pacientes/{id}", id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.nome").value("Maria Silva Souza"))
                .andExpect(jsonPath("$.convenio").value("Amil"))
                .andExpect(jsonPath("$.alergias").value("Dipirona"))
                .andExpect(jsonPath("$.prontuario").value("Hipertensa"));
    }

    @Test
    @DisplayName("Medico recebe o dado clinico normalmente")
    void medicoRecebeDadoClinico() throws Exception {
        String admin = tokenDe(Usuario.Perfil.ADMINISTRADOR);
        mockMvc.perform(post("/pacientes")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Maria Silva","cpf":"11122233344","alergias":"Dipirona"}
                        """));

        String medico = tokenDe(Usuario.Perfil.MEDICO);

        mockMvc.perform(get("/pacientes").header("Authorization", bearer(medico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alergias").value("Dipirona"));
    }

    /* ----------------- regra de negocio continua valendo -------------- */

    @Test
    @DisplayName("Com token valido as regras de negocio seguem as mesmas")
    void regraDeNegocioContinuaValendoComToken() throws Exception {
        String token = tokenDe(Usuario.Perfil.ADMINISTRADOR);

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva","cpf":"11122233344"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/pacientes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outra Pessoa","cpf":"11122233344"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pacienteId":1,"medicoId":1,"data":"%s","horario":"09:30"}
                                """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest());
    }
}
