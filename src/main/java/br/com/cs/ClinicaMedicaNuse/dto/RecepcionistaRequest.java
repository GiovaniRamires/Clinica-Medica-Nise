package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados de entrada para criar ou atualizar uma recepcionista. */
public record RecepcionistaRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "A matrícula é obrigatória")
        @Pattern(regexp = "\\d{4,20}", message = "A matrícula deve conter apenas dígitos (4 a 20)")
        String matricula,

        @NotBlank(message = "O turno é obrigatório")
        @Pattern(regexp = "MANHÃ|TARDE|NOITE", message = "O turno deve ser MANHÃ, TARDE ou NOITE")
        String turno,

        @NotNull(message = "O id do usuário é obrigatório")
        Long usuarioId
) {
}
