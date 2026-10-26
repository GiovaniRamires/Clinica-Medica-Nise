package br.com.cs.ClinicaMedicaNuse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;

public record HorarioDisponivelResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,
        boolean livre
) {
    public record Agenda(
            int totalSlots,
            int livres,
            int duracaoMinutos,
            List<HorarioDisponivelResponse> horarios
    ) {
    }
}