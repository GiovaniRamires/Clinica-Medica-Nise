package br.com.cs.ClinicaMedicaNuse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitação de agendamento feita pelo próprio paciente logado.
 * O pacienteId não vem no corpo: é resolvido a partir do usuário autenticado,
 * nunca de um id que o cliente poderia manipular. Vira um Agendamento com
 * situação SOLICITADO, que a recepção/administrador confirma depois.
 */
public record SolicitacaoAgendamentoRequest(

        @NotNull(message = "O id do médico é obrigatório")
        Long medicoId,

        @NotNull(message = "A data é obrigatória")
        @FutureOrPresent(message = "Não é possível solicitar uma data passada")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate data,

        @NotNull(message = "O horário é obrigatório")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,

        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
        String observacoes
) {
}
