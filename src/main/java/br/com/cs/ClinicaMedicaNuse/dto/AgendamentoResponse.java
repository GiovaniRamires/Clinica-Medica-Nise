package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Representação do agendamento devolvida pela API. */
public record AgendamentoResponse(
        Long id,
        PacienteResumoResponse paciente,
        MedicoResponse medico,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate data,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,

        Agendamento.SituacaoAgendamento situacao,
        String observacoes
) {
    public static AgendamentoResponse from(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                PacienteResumoResponse.from(agendamento.getPaciente()),
                MedicoResponse.from(agendamento.getMedico()),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getSituacao(),
                agendamento.getObservacoes());
    }

    public static List<AgendamentoResponse> from(List<Agendamento> agendamentos) {
        return agendamentos.stream().map(AgendamentoResponse::from).toList();
    }
}
