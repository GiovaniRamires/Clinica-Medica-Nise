package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Representação da consulta devolvida pela API. Expõe o agendamento de
 * origem apenas pelo id, evitando devolver o grafo inteiro de entidades.
 */
public record ConsultaResponse(
        Long id,
        PacienteResumoResponse paciente,
        MedicoResponse medico,
        Long agendamentoId,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate data,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,

        String queixaPrincipal,
        String observacoesMedicas,
        String diagnostico,
        String conduta
) {
    public static ConsultaResponse from(Consulta consulta) {
        return new ConsultaResponse(
                consulta.getId(),
                PacienteResumoResponse.from(consulta.getPaciente()),
                MedicoResponse.from(consulta.getMedico()),
                consulta.getAgendamento() != null ? consulta.getAgendamento().getId() : null,
                consulta.getData(),
                consulta.getHorario(),
                consulta.getQueixaPrincipal(),
                consulta.getObservacoesMedicas(),
                consulta.getDiagnostico(),
                consulta.getConduta());
    }

    public static List<ConsultaResponse> from(List<Consulta> consultas) {
        return consultas.stream().map(ConsultaResponse::from).toList();
    }
}
