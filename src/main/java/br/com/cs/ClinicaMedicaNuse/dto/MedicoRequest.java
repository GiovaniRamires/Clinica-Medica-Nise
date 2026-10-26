package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados de entrada para criar ou atualizar um médico. */
public record MedicoRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O CRM é obrigatório")
        @Pattern(regexp = "\\d{4,6}(/[A-Z]{2})?",
                message = "CRM deve ter de 4 a 6 dígitos, opcionalmente seguido de /UF (ex: 123456/SP)")
        String crm,

        @NotBlank(message = "A especialidade é obrigatória")
        @Size(max = 80, message = "A especialidade deve ter no máximo 80 caracteres")
        String especialidade,

        @NotNull(message = "O id do usuário é obrigatório")
        Long usuarioId
) {
}
