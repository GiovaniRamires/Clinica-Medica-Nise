package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para registrar o atendimento de um agendamento.
 * Paciente, médico, data e horário não são enviados pelo cliente: são
 * derivados do agendamento no momento do registro.
 */
public record ConsultaRequest(

        @NotNull(message = "O id do agendamento é obrigatório")
        Long agendamentoId,

        @NotBlank(message = "A queixa principal é obrigatória")
        @Size(max = 500, message = "A queixa principal deve ter no máximo 500 caracteres")
        String queixaPrincipal,

        @Size(max = 2000, message = "As observações médicas devem ter no máximo 2000 caracteres")
        String observacoesMedicas,

        @Size(max = 2000, message = "O diagnóstico deve ter no máximo 2000 caracteres")
        String diagnostico,

        @Size(max = 2000, message = "A conduta deve ter no máximo 2000 caracteres")
        String conduta
) {
}
