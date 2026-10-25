package br.com.cs.ClinicaMedicaNuse;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de ponta a ponta da API: garantem o contrato dos DTOs (o que entra,
 * o que sai e o que NÃO sai), a validação dos campos e as regras de negócio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String AMANHA = LocalDate.now().plusDays(1).toString();

    /* --------------------------- utilidades --------------------------- */

    private String postar(String caminho, String corpo, int statusEsperado) throws Exception {
        return mockMvc.perform(post(caminho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().is(statusEsperado))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static long id(String conteudo) {
        return ((Number) JsonPath.read(conteudo, "$.id")).longValue();
    }

    private static <T> T ler(String conteudo, String caminho) {
        return JsonPath.read(conteudo, caminho);
    }

    private long criarPaciente(String nome, String cpf) throws Exception {
        return id(postar("/pacientes", """
                {"nome":"%s","cpf":"%s","convenio":"Unimed","alergias":"Dipirona",
                 "prontuario":"Hipertenso desde 2020"}
                """.formatted(nome, cpf), 201));
    }

    private long criarMedico(String nome, String crm) throws Exception {
        return id(postar("/medicos", """
                {"nome":"%s","crm":"%s","especialidade":"Cardiologia"}
                """.formatted(nome, crm), 201));
    }

    private long criarAgendamento(long pacienteId, long medicoId, String horario) throws Exception {
        return id(postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"%s","observacoes":"Retorno"}
                """.formatted(pacienteId, medicoId, AMANHA, horario), 201));
    }

    /* ---------------------------- pacientes --------------------------- */

    @Test
    @DisplayName("POST /pacientes devolve o PacienteResponse com os campos do DTO")
    void criaPacienteComContratoDoDto() throws Exception {
        String corpo = postar("/pacientes", """
                {"nome":"Maria Silva","cpf":"11122233344","convenio":"Unimed",
                 "alergias":"Dipirona","prontuario":"Hipertensa"}
                """, 201);

        assertThat(id(corpo)).isPositive();
        assertThat((String) ler(corpo, "$.nome")).isEqualTo("Maria Silva");
        assertThat((String) ler(corpo, "$.cpf")).isEqualTo("11122233344");
        assertThat(((Map<String, Object>) ler(corpo, "$")).keySet())
                .containsExactlyInAnyOrder("id", "nome", "cpf", "convenio", "alergias", "prontuario");
    }

    @Test
    @DisplayName("CPF fora do formato cai na validação do DTO, com o campo apontado")
    void recusaCpfInvalido() throws Exception {
        String erro = postar("/pacientes", """
                {"nome":"Maria Silva","cpf":"123"}
                """, 400);

        assertThat((Integer) ler(erro, "$.status")).isEqualTo(400);
        assertThat((String) ler(erro, "$.campos[0].campo")).isEqualTo("cpf");
    }

    @Test
    @DisplayName("Nome em branco cai na validação do DTO")
    void recusaNomeEmBranco() throws Exception {
        String erro = postar("/pacientes", """
                {"nome":"   ","cpf":"11122233344"}
                """, 400);

        assertThat((String) ler(erro, "$.campos[0].campo")).isEqualTo("nome");
    }

    @Test
    @DisplayName("CPF repetido no cadastro devolve 409")
    void recusaCpfDuplicadoNoCadastro() throws Exception {
        criarPaciente("Maria Silva", "11122233344");

        String erro = postar("/pacientes", """
                {"nome":"Outra Pessoa","cpf":"11122233344"}
                """, 409);

        assertThat((String) ler(erro, "$.mensagem")).contains("CPF");
    }

    @Test
    @DisplayName("PUT não deixa um paciente assumir o CPF de outro")
    void recusaCpfDuplicadoNaAtualizacao() throws Exception {
        criarPaciente("Maria Silva", "11122233344");
        long segundoId = criarPaciente("Joao Souza", "55566677788");

        mockMvc.perform(put("/pacientes/{id}", segundoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Joao Souza","cpf":"11122233344"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT mantendo o próprio CPF funciona")
    void permiteAtualizarMantendoOProprioCpf() throws Exception {
        long id = criarPaciente("Maria Silva", "11122233344");

        mockMvc.perform(put("/pacientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Silva Souza","cpf":"11122233344","convenio":"Amil"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva Souza"))
                .andExpect(jsonPath("$.convenio").value("Amil"));
    }

    @Test
    @DisplayName("Paciente inexistente devolve 404 no formato ErroResponse")
    void devolve404ComCorpoPadrao() throws Exception {
        mockMvc.perform(get("/pacientes/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.caminho").value("/pacientes/999999"));
    }

    /* ----------------------------- médicos ---------------------------- */

    @Test
    @DisplayName("CRM repetido devolve 409")
    void recusaCrmDuplicado() throws Exception {
        criarMedico("Dr. House", "123456");

        String erro = postar("/medicos", """
                {"nome":"Dra. Cuddy","crm":"123456","especialidade":"Endocrinologia"}
                """, 409);

        assertThat((String) ler(erro, "$.mensagem")).contains("CRM");
    }

    @Test
    @DisplayName("CRM fora do padrão cai na validação do DTO")
    void recusaCrmInvalido() throws Exception {
        String erro = postar("/medicos", """
                {"nome":"Dr. House","crm":"abc","especialidade":"Clinico Geral"}
                """, 400);

        assertThat((String) ler(erro, "$.campos[0].campo")).isEqualTo("crm");
    }

    /* ---------------------------- agendamento -------------------------- */

    @Test
    @DisplayName("AgendamentoResponse não expõe dados clínicos do paciente")
    void agendamentoNaoVazaDadoClinico() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");

        String corpo = postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"09:30"}
                """.formatted(pacienteId, medicoId, AMANHA), 201);

        assertThat((String) ler(corpo, "$.situacao")).isEqualTo("AGENDADO");
        assertThat((String) ler(corpo, "$.horario")).isEqualTo("09:30");
        assertThat((String) ler(corpo, "$.data")).isEqualTo(AMANHA);
        assertThat(((Map<String, Object>) ler(corpo, "$.paciente")).keySet())
                .containsExactlyInAnyOrder("id", "nome", "cpf");
    }

    @Test
    @DisplayName("Data no passado cai na validação do DTO")
    void recusaAgendamentoNoPassado() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");

        String erro = postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"09:30"}
                """.formatted(pacienteId, medicoId, LocalDate.now().minusDays(1)), 400);

        assertThat((String) ler(erro, "$.campos[0].campo")).isEqualTo("data");
    }

    @Test
    @DisplayName("Médico não pode ter dois agendamentos no mesmo horário")
    void recusaHorarioOcupadoDoMedico() throws Exception {
        long medicoId = criarMedico("Dr. House", "123456");
        long primeiro = criarPaciente("Maria Silva", "11122233344");
        long segundo = criarPaciente("Joao Souza", "55566677788");

        criarAgendamento(primeiro, medicoId, "09:30");

        String erro = postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"09:30"}
                """.formatted(segundo, medicoId, AMANHA), 409);

        assertThat((String) ler(erro, "$.mensagem")).contains("agendamento");
    }

    @Test
    @DisplayName("Paciente não pode ter dois agendamentos no mesmo horário")
    void recusaHorarioOcupadoDoPaciente() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long primeiroMedico = criarMedico("Dr. House", "123456");
        long segundoMedico = criarMedico("Dra. Cuddy", "654321");

        criarAgendamento(pacienteId, primeiroMedico, "09:30");

        postar("/agendamentos", """
                {"pacienteId":%d,"medicoId":%d,"data":"%s","horario":"09:30"}
                """.formatted(pacienteId, segundoMedico, AMANHA), 409);
    }

    @Test
    @DisplayName("Situação inválida no corpo devolve 400 em vez de erro de conversão")
    void recusaSituacaoInvalida() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        mockMvc.perform(put("/agendamentos/{id}/situacao", agendamentoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"situacao":"INVENTADA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Agendamento confirmado pode ser marcado como realizado")
    void confirmadoPodeVirarRealizado() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        mockMvc.perform(put("/agendamentos/{id}/situacao", agendamentoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"situacao":"CONFIRMADO"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/agendamentos/{id}/realizado", agendamentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REALIZADO"));
    }

    /* ----------------------- consulta e prontuário --------------------- */

    @Test
    @DisplayName("Consulta registrada fecha o agendamento e entra no prontuário")
    void consultaEntraNoProntuario() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        String consulta = postar("/consultas", """
                {"agendamentoId":%d,"queixaPrincipal":"Dor no peito",
                 "diagnostico":"Angina estavel","conduta":"Repouso e retorno em 15 dias",
                 "observacoesMedicas":"Paciente ansioso"}
                """.formatted(agendamentoId), 201);

        assertThat(((Number) ler(consulta, "$.agendamentoId")).longValue()).isEqualTo(agendamentoId);
        assertThat(((Number) ler(consulta, "$.paciente.id")).longValue()).isEqualTo(pacienteId);
        assertThat(((Number) ler(consulta, "$.medico.id")).longValue()).isEqualTo(medicoId);

        mockMvc.perform(get("/agendamentos/{id}", agendamentoId))
                .andExpect(jsonPath("$.situacao").value("REALIZADO"));

        mockMvc.perform(get("/pacientes/{id}/prontuario", pacienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConsultas").value(1))
                .andExpect(jsonPath("$.paciente.prontuario").value("Hipertenso desde 2020"))
                .andExpect(jsonPath("$.consultas[0].queixaPrincipal").value("Dor no peito"))
                .andExpect(jsonPath("$.consultas[0].conduta").value("Repouso e retorno em 15 dias"));
    }

    @Test
    @DisplayName("Queixa principal é obrigatória na consulta")
    void recusaConsultaSemQueixa() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        String erro = postar("/consultas", """
                {"agendamentoId":%d,"diagnostico":"Sem queixa"}
                """.formatted(agendamentoId), 400);

        assertThat((String) ler(erro, "$.campos[0].campo")).isEqualTo("queixaPrincipal");
    }

    @Test
    @DisplayName("Agendamento cancelado não aceita consulta")
    void recusaConsultaDeAgendamentoCancelado() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        mockMvc.perform(put("/agendamentos/{id}/situacao", agendamentoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"situacao":"CANCELADO"}
                                """))
                .andExpect(status().isOk());

        postar("/consultas", """
                {"agendamentoId":%d,"queixaPrincipal":"Dor no peito"}
                """.formatted(agendamentoId), 409);
    }

    @Test
    @DisplayName("PUT /consultas/{id} atualiza apenas os campos clínicos")
    void atualizaConsulta() throws Exception {
        long pacienteId = criarPaciente("Maria Silva", "11122233344");
        long medicoId = criarMedico("Dr. House", "123456");
        long agendamentoId = criarAgendamento(pacienteId, medicoId, "09:30");

        long consultaId = id(postar("/consultas", """
                {"agendamentoId":%d,"queixaPrincipal":"Dor no peito"}
                """.formatted(agendamentoId), 201));

        mockMvc.perform(put("/consultas/{id}", consultaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"queixaPrincipal":"Dor no peito ao esforco","diagnostico":"Angina",
                                 "conduta":"Encaminhar ao cardiologista"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Angina"))
                .andExpect(jsonPath("$.agendamentoId").value(agendamentoId));
    }

    /* ------------------------------ front-end -------------------------- */

    @Test
    @DisplayName("A raiz redireciona para a interface web")
    void raizRedirecionaParaFrontEnd() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/index.html"));
    }
}
