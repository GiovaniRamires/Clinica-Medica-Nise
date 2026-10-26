package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExameRequest(

        @NotNull(message = "A consulta de origem é obrigatória")
        Long consultaId,

        @NotBlank(message = "O nome do exame é obrigatório")
        @Size(max = 150, message = "O nome do exame deve ter no máximo 150 caracteres")
        String nome
) {
}
