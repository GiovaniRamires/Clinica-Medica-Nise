package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import jakarta.validation.constraints.NotNull;

public record AlterarSituacaoExameRequest(

        @NotNull(message = "A situação é obrigatória")
        Exame.SituacaoExame situacao
) {
}
