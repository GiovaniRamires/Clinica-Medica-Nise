package br.com.cs.ClinicaMedicaNuse;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import br.com.cs.ClinicaMedicaNuse.repository.UsuarioRepository;
import br.com.cs.ClinicaMedicaNuse.security.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Relatórios/consultas gerenciais (PA-19). Monta um pequeno cenário (consulta
 * realizada + uma falta + um exame) e confere que as agregações do endpoint
 * GET /relatorios/gerencial batem com os dados criados. Também verifica que o
 * paciente não acessa o relatório.
 */
@SpringBootTest
@Transactional
class RelatorioIntegracaoTest {

    @Autowired
    private WebApplicationContext contexto;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenService tokenService;

    private MockMvc mockMvc;

    private static final String AMANHA = LocalDate.now().plusDays(1).toString();

    @BeforeEach
    void preparar() {
        var admin = usuarioRepository.findByEmail("teste-admin@clinicanise.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario(
                        "Admin de teste", "teste-admin@clinicanise.local",
                        passwordEncoder.encode("senhaDeTeste123"), Usuario.Perfil.ADMINISTRADOR)));
        String autorizacaoAdmin = "Bearer " + tokenService.gerar(admin);

        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .defaultRequest(get("/").header("Authorization", autorizacaoAdmin))
                .build();
    }

    /* --------------------------- utilidades --------------------------- */

    private String postar(String caminho, String corpo, int status) throws Exception {
        return mockMvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }

    private static long id(String conteudo) {
        return ((Number) JsonPath.read(conteudo, "$.id")).longValue();
    }

    private long criarPaciente(String nome, String cpf) throws Exception {
        return id(postar("/pacientes", """
                {"nome":"%s","cpf":"%s","convenio":"Unimed","alergias":"-","prontuario":"-"}
                """.formatted(nome, cpf), 201));
    }

    private long criarMedico(String nome, String crm) throws Exception {
        long usuarioId = usuarioRepository.save(new Usuario(
                "Usuario " + crm, "medico-" + crm + "@clinicanise.local",
                passwordEncoder.encode("senhaDeTeste123"), Usuario.Perfil.MEDICO)).getId();
        return id(postar("/medicos", """
                {"nome":"%s","crm":"%s","especialidade":"Cardiologia","usuarioId":%d}
                """.formatted(nome, crm, usuarioId), 201));
    }

    private long criarAgendamento(long pacienteId, long medicoId, String horario) throws Exception {
        return id(postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"%s","observacoes":"-"}
                """.formatted(pacienteId, medicoId, AMANHA, horario), 201));
    }

    /* ----------------------------- testes ----------------------------- */

    @Test
    @DisplayName("GET /relatorios/gerencial agrega consultas, faltas e exames")
    void agregaIndicadores() throws Exception {
        long medicoId = criarMedico("Dr. House", "123456");
        long p1 = criarPaciente("Maria Silva", "11122233344");
        long p2 = criarPaciente("Ana Oliveira", "55566677788");

        // Paciente 1: consulta realizada (gera 1 consulta + 1 agendamento REALIZADO).
        long ag1 = criarAgendamento(p1, medicoId, "09:00");
        String consulta = postar("/consultas", """
                {"agendamentoId":%d,"queixaPrincipal":"Dor no peito","diagnostico":"Angina",
                 "conduta":"Repouso","observacoesMedicas":"-"}
                """.formatted(ag1), 201);
        long consultaId = id(consulta);

        // Exame solicitado nessa consulta.
        postar("/exames", """
                {"consultaId":%d,"nome":"Hemograma"}
                """.formatted(consultaId), 201);

        // Paciente 2: falta (agendamento marcado como AUSENTE).
        long ag2 = criarAgendamento(p2, medicoId, "10:00");
        mockMvc.perform(put("/agendamentos/{id}/situacao", ag2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"situacao\":\"AUSENTE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/relatorios/gerencial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPacientes").value(2))
                .andExpect(jsonPath("$.totalMedicos").value(1))
                .andExpect(jsonPath("$.totalConsultasRealizadas").value(1))
                .andExpect(jsonPath("$.totalExames").value(1))
                .andExpect(jsonPath("$.totalFaltas").value(1))
                // 1 realizado e 1 falta -> 50% comparecimento, 50% faltas.
                .andExpect(jsonPath("$.taxaComparecimento").value(50.0))
                .andExpect(jsonPath("$.taxaFaltas").value(50.0))
                .andExpect(jsonPath("$.consultasPorMedico[0].quantidade").value(1))
                .andExpect(jsonPath("$.examesMaisSolicitados[0].rotulo").value("Hemograma"));
    }

    @Test
    @DisplayName("Paciente não acessa relatórios gerenciais (403)")
    void pacienteNaoAcessa() throws Exception {
        var paciente = usuarioRepository.save(new Usuario(
                "Paciente teste", "paciente-rel@clinicanise.local",
                passwordEncoder.encode("senhaDeTeste123"), Usuario.Perfil.PACIENTE));
        String token = "Bearer " + tokenService.gerar(paciente);

        mockMvc.perform(get("/relatorios/gerencial").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}
