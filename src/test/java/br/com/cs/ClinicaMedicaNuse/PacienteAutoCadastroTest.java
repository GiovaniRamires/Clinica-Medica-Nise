package br.com.cs.ClinicaMedicaNuse;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import br.com.cs.ClinicaMedicaNuse.security.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do auto-cadastro de paciente e da agenda do próprio paciente:
 * POST /auth/cadastro-paciente cria Usuario(PACIENTE) + Paciente vinculados,
 * e o paciente logado só vê/solicita a própria agenda, nunca a de outro
 * paciente nem o dado clínico administrativo.
 */
@SpringBootTest
@Transactional
class PacienteAutoCadastroTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private MockMvc mockMvcSemToken;

    private static final String AMANHA = LocalDate.now().plusDays(1).toString();

    /* --------------------------- utilidades --------------------------- */

    private MockMvc mockMvc() {
        if (mockMvcSemToken == null) {
            mockMvcSemToken = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        }
        return mockMvcSemToken;
    }

    private MockMvc mockMvcComAdmin() {
        var admin = usuarioRepository.findByEmail("teste-admin@clinicanise.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario(
                        "Admin de teste", "teste-admin@clinicanise.local",
                        passwordEncoder.encode("senhaDeTeste123"), Usuario.Perfil.ADMINISTRADOR)));
        String autorizacao = "Bearer " + tokenService.gerar(admin);
        return MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity())
                .defaultRequest(get("/").header("Authorization", autorizacao)).build();
    }

    /** Médico exige um usuário de perfil MEDICO: cria o login e devolve o id. */
    private long criarUsuarioMedico() {
        return usuarioRepository.save(new Usuario(
                "Dr. House", "house@clinicanise.local",
                passwordEncoder.encode("senhaDeTeste123"), Usuario.Perfil.MEDICO)).getId();
    }

    private String cadastrarPaciente(String nome, String cpf, String email, String senha) throws Exception {
        return mockMvc().perform(post("/auth/cadastro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","cpf":"%s","email":"%s","senha":"%s"}
                                """.formatted(nome, cpf, email, senha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /* ----------------------------- testes ------------------------------ */

    @Test
    @DisplayName("Auto-cadastro cria Usuario e Paciente vinculados e devolve login pronto")
    void autoCadastroCriaUsuarioEPacienteVinculados() throws Exception {
        String corpo = cadastrarPaciente("Carlos Andrade", "12312312312",
                "carlos@paciente.local", "senhaForte123");

        assertThat((String) JsonPath.read(corpo, "$.token")).isNotBlank();
        assertThat((String) JsonPath.read(corpo, "$.usuario.perfil")).isEqualTo("PACIENTE");
        assertThat((String) JsonPath.read(corpo, "$.usuario.email")).isEqualTo("carlos@paciente.local");
        assertThat(corpo).doesNotContain("senhaForte123").doesNotContain("$2a$").doesNotContain("$2b$");

        var usuario = usuarioRepository.findByEmail("carlos@paciente.local").orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(Usuario.Perfil.PACIENTE);
    }

    @Test
    @DisplayName("CPF ou e-mail repetido no auto-cadastro devolve 409")
    void autoCadastroRecusaCpfOuEmailDuplicado() throws Exception {
        cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123");

        mockMvc().perform(post("/auth/cadastro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Nome","cpf":"12312312312","email":"outro@paciente.local","senha":"senhaForte123"}
                                """))
                .andExpect(status().isConflict());

        mockMvc().perform(post("/auth/cadastro-paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro Nome","cpf":"99988877766","email":"carlos@paciente.local","senha":"senhaForte123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Paciente logado solicita agendamento e ele nasce em SOLICITADO")
    void pacienteSolicitaAgendamento() throws Exception {
        String corpoCadastro = cadastrarPaciente("Carlos Andrade", "12312312312",
                "carlos@paciente.local", "senhaForte123");
        String tokenPaciente = JsonPath.read(corpoCadastro, "$.token");

        long medicoId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                                """.formatted(criarUsuarioMedico())))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        String corpo = mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicoId":%d,"data":"%s","horario":"09:30","observacoes":"Dor de cabeca"}
                                """.formatted(medicoId, AMANHA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(corpo, "$.situacao")).isEqualTo("SOLICITADO");
    }

    @Test
    @DisplayName("Minhas-consultas so mostra a agenda do proprio paciente, nunca de outro")
    void minhasConsultasSoMostraAsProprias() throws Exception {
        String tokenCarlos = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");
        String tokenAna = JsonPath.read(
                cadastrarPaciente("Ana Oliveira", "32132132132", "ana@paciente.local", "senhaForte123"),
                "$.token");

        long medicoId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                                """.formatted(criarUsuarioMedico())))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        mockMvc().perform(post("/auth/solicitar-agendamento")
                .header("Authorization", bearer(tokenCarlos))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"medicoId":%d,"data":"%s","horario":"09:30"}
                        """.formatted(medicoId, AMANHA)));

        mockMvc().perform(get("/auth/minhas-consultas").header("Authorization", bearer(tokenCarlos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paciente.nome").value("Carlos Andrade"));

        mockMvc().perform(get("/auth/minhas-consultas").header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Paciente cancela a propria consulta e o horario volta a ficar livre; consulta de outro da 404")
    void pacienteCancelaPropriaConsulta() throws Exception {
        String tokenCarlos = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");
        String tokenAna = JsonPath.read(
                cadastrarPaciente("Ana Oliveira", "32132132132", "ana@paciente.local", "senhaForte123"),
                "$.token");

        long medicoId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                                """.formatted(criarUsuarioMedico())))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        long agendamentoId = Long.parseLong(JsonPath.read(mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenCarlos))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicoId":%d,"data":"%s","horario":"09:30"}
                                """.formatted(medicoId, AMANHA)))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        // horario ocupado: a Ana nao consegue pedir o mesmo
        String pedidoAna = """
                {"medicoId":%d,"data":"%s","horario":"09:30"}
                """.formatted(medicoId, AMANHA);
        mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON).content(pedidoAna))
                .andExpect(status().isConflict());

        // a Ana nao cancela a consulta do Carlos (e nem descobre que ela existe)
        mockMvc().perform(put("/auth/minhas-consultas/{id}/cancelar", agendamentoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNotFound());

        // o Carlos cancela a dele
        mockMvc().perform(put("/auth/minhas-consultas/{id}/cancelar", agendamentoId)
                        .header("Authorization", bearer(tokenCarlos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"));

        // horario liberado: agora a Ana consegue
        mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON).content(pedidoAna))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Paciente ve o retorno do atendimento (diagnostico, prescricao, exame) sem as anotacoes internas")
    void pacienteVeRetornoDoAtendimento() throws Exception {
        String tokenCarlos = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");
        String tokenAna = JsonPath.read(
                cadastrarPaciente("Ana Oliveira", "32132132132", "ana@paciente.local", "senhaForte123"),
                "$.token");

        long medicoId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                                """.formatted(criarUsuarioMedico())))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        long agendamentoId = Long.parseLong(JsonPath.read(mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenCarlos))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicoId":%d,"data":"%s","horario":"09:30"}
                                """.formatted(medicoId, AMANHA)))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        mockMvcComAdmin().perform(put("/agendamentos/{id}/situacao", agendamentoId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"AGENDADO\"}"));

        long consultaId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agendamentoId":%d,"queixaPrincipal":"Dor de cabeca",
                                 "observacoesMedicas":"ANOTACAO INTERNA DA EQUIPE",
                                 "diagnostico":"Enxaqueca","conduta":"Repouso e hidratacao"}
                                """.formatted(agendamentoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        mockMvcComAdmin().perform(post("/prescricoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consultaId":%d,"medicamento":"Dipirona","dosagem":"500mg",
                                 "frequencia":"8/8h","duracao":"5 dias"}
                                """.formatted(consultaId)))
                .andExpect(status().isCreated());
        mockMvcComAdmin().perform(post("/exames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consultaId":%d,"nome":"Hemograma"}
                                """.formatted(consultaId)))
                .andExpect(status().isCreated());

        String corpo = mockMvc().perform(get("/auth/meus-atendimentos").header("Authorization", bearer(tokenCarlos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].medico.nome").value("Dr. House"))
                .andExpect(jsonPath("$[0].diagnostico").value("Enxaqueca"))
                .andExpect(jsonPath("$[0].conduta").value("Repouso e hidratacao"))
                .andExpect(jsonPath("$[0].prescricoes[0].medicamento").value("Dipirona"))
                .andExpect(jsonPath("$[0].exames[0].nome").value("Hemograma"))
                .andExpect(jsonPath("$[0].exames[0].situacao").value("SOLICITADO"))
                .andReturn().getResponse().getContentAsString();

        // anotacao interna da equipe e ids de usuario nunca saem para o paciente
        assertThat(corpo).doesNotContain("observacoesMedicas").doesNotContain("ANOTACAO INTERNA");
        assertThat(corpo).doesNotContain("usuarioId");

        // outro paciente nao ve nada disso
        mockMvc().perform(get("/auth/meus-atendimentos").header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Meus-atendimentos so para PACIENTE")
    void meusAtendimentosExigePerfilPaciente() throws Exception {
        mockMvcComAdmin().perform(get("/auth/meus-atendimentos")).andExpect(status().isForbidden());
        mockMvc().perform(get("/auth/meus-atendimentos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cancelamento pelo paciente exige perfil PACIENTE")
    void cancelarMinhaConsultaExigePerfilPaciente() throws Exception {
        mockMvcComAdmin().perform(put("/auth/minhas-consultas/{id}/cancelar", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Recepcao confirma a solicitacao: SOLICITADO vira AGENDADO")
    void recepcaoConfirmaSolicitacao() throws Exception {
        String tokenPaciente = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");

        long medicoId = Long.parseLong(JsonPath.read(mockMvcComAdmin().perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                                """.formatted(criarUsuarioMedico())))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        long agendamentoId = Long.parseLong(JsonPath.read(mockMvc().perform(post("/auth/solicitar-agendamento")
                        .header("Authorization", bearer(tokenPaciente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicoId":%d,"data":"%s","horario":"09:30"}
                                """.formatted(medicoId, AMANHA)))
                .andReturn().getResponse().getContentAsString(), "$.id").toString());

        mockMvcComAdmin().perform(put("/agendamentos/{id}/situacao", agendamentoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"situacao":"AGENDADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADO"));
    }

    @Test
    @DisplayName("Paciente nao acessa rotas administrativas de pacientes/medicos/agendamentos")
    void pacienteNaoAcessaRotasDeStaff() throws Exception {
        String tokenPaciente = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");

        mockMvc().perform(get("/pacientes").header("Authorization", bearer(tokenPaciente)))
                .andExpect(status().isForbidden());
        mockMvc().perform(get("/agendamentos").header("Authorization", bearer(tokenPaciente)))
                .andExpect(status().isForbidden());
        mockMvc().perform(get("/usuarios").header("Authorization", bearer(tokenPaciente)))
                .andExpect(status().isForbidden());
        mockMvc().perform(post("/medicos").header("Authorization", bearer(tokenPaciente)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Paciente le a lista de medicos para montar o select de solicitar consulta")
    void pacienteLeListaDeMedicosParaSolicitar() throws Exception {
        String tokenPaciente = JsonPath.read(
                cadastrarPaciente("Carlos Andrade", "12312312312", "carlos@paciente.local", "senhaForte123"),
                "$.token");

        mockMvcComAdmin().perform(post("/medicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Dr. House","crm":"123456","especialidade":"Clinica Geral","usuarioId":%d}
                        """.formatted(criarUsuarioMedico())));

        mockMvc().perform(get("/medicos").header("Authorization", bearer(tokenPaciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Dr. House"));
    }

    @Test
    @DisplayName("Administrador nao pode criar nem promover usuario para o perfil PACIENTE")
    void adminNaoCriaUsuarioPaciente() throws Exception {
        mockMvcComAdmin().perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Alguem","email":"alguem@clinicanise.local","senha":"senhaboa123","perfil":"PACIENTE"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
