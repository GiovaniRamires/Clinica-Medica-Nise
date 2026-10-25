package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados editáveis de uma consulta já registrada. O agendamento de origem
 * não pode ser trocado depois do registro, por isso não aparece aqui.
 */
public record ConsultaAtualizacaoRequest(

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
