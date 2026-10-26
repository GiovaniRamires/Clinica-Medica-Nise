package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Exame;

import java.time.LocalDate;
import java.util.List;

public record ExameResponse(
        Long id,
        PacienteResumoResponse paciente,
        Long consultaId,
        String nome,
        LocalDate dataSolicitacao,
        Exame.SituacaoExame situacao,
        LocalDate dataRealizacao,
        String resultado
) {
    public static ExameResponse from(Exame exame) {
        return new ExameResponse(
                exame.getId(),
                PacienteResumoResponse.from(exame.getPaciente()),
                exame.getConsulta().getId(),
                exame.getNome(),
                exame.getDataSolicitacao(),
                exame.getSituacao(),
                exame.getDataRealizacao(),
                exame.getResultado());
    }

    public static List<ExameResponse> from(List<Exame> exames) {
        return exames.stream().map(ExameResponse::from).toList();
    }
}
