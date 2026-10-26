package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResultadoExameRequest(

        @NotBlank(message = "O resultado é obrigatório")
        @Size(max = 2000, message = "O resultado deve ter no máximo 2000 caracteres")
        String resultado
) {
}
