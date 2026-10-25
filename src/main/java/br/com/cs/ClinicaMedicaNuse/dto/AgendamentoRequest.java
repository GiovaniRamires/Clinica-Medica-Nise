package br.com.cs.ClinicaMedicaNuse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados de entrada para marcar um agendamento.
 * O cliente envia apenas os ids do paciente e do médico: o relacionamento
 * é resolvido no service, e não por um objeto aninhado vindo do JSON.
 */
public record AgendamentoRequest(

        @NotNull(message = "O id do paciente é obrigatório")
        Long pacienteId,

        @NotNull(message = "O id do médico é obrigatório")
        Long medicoId,

        @NotNull(message = "A data é obrigatória")
        @FutureOrPresent(message = "Não é possível agendar em uma data passada")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate data,

        @NotNull(message = "O horário é obrigatório")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,

        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
        String observacoes
) {
}
