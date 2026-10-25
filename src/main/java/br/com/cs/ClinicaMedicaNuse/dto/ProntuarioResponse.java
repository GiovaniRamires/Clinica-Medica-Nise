package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;

import java.util.List;

/**
 * Prontuário do paciente: seus dados clínicos mais o histórico de consultas,
 * já no formato de DTO (antes este record devolvia as entidades JPA direto).
 */
public record ProntuarioResponse(
        PacienteResponse paciente,
        int totalConsultas,
        List<ConsultaResponse> consultas
) {
    public static ProntuarioResponse from(Paciente paciente, List<Consulta> consultas) {
        var historico = ConsultaResponse.from(consultas);
        return new ProntuarioResponse(PacienteResponse.from(paciente), historico.size(), historico);
    }
}
