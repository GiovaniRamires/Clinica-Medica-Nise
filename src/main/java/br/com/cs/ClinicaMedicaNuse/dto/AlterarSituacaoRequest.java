package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import jakarta.validation.constraints.NotNull;

/**
 * Nova situação de um agendamento. Vem no corpo da requisição em vez de
 * query param, para que um valor inválido caia na validação da API e não
 * em uma falha de conversão de parâmetro.
 */
public record AlterarSituacaoRequest(

        @NotNull(message = "A situação é obrigatória")
        Agendamento.SituacaoAgendamento situacao
) {
}
