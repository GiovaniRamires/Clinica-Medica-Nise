package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record PrescricaoRequest(

        @NotNull(message = "O id da consulta é obrigatório")
        Long consultaId,

        @NotBlank(message = "O medicamento é obrigatório")
        @Size(max = 200, message = "O medicamento deve ter no máximo 200 caracteres")
        String medicamento,

        @NotBlank(message = "A dosagem é obrigatória")
        @Size(max = 100, message = "A dosagem deve ter no máximo 100 caracteres")
        String dosagem,

        @NotBlank(message = "A frequência é obrigatória")
        @Size(max = 100, message = "A frequência deve ter no máximo 100 caracteres")
        String frequencia,

        @Size(max = 100, message = "A duração deve ter no máximo 100 caracteres")
        String duracao,

        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
        String observacoes
) {
}
