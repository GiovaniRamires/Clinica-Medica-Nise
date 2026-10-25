package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para criar ou atualizar um paciente.
 * O id nunca vem do cliente: ele é definido pela URL (PUT) ou pelo banco (POST).
 */
public record PacienteRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos!")
        String cpf,

        @Size(max = 80, message = "O convênio deve ter no máximo 80 caracteres")
        String convenio,

        @Size(max = 500, message = "As alergias devem ter no máximo 500 caracteres")
        String alergias,

        @Size(max = 2000, message = "O prontuário deve ter no máximo 2000 caracteres")
        String prontuario
) {
}
