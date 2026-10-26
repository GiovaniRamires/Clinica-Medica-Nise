package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Prescricao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record PrescricaoResponse(
        Long id,
        Long consultaId,
        String medicamento,
        String dosagem,
        String frequencia,
        String duracao,
        String observacoes,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime criadoEm
) {
    public static PrescricaoResponse from(Prescricao p) {
        return new PrescricaoResponse(
                p.getId(),
                p.getConsulta() != null ? p.getConsulta().getId() : null,
                p.getMedicamento(),
                p.getDosagem(),
                p.getFrequencia(),
                p.getDuracao(),
                p.getObservacoes(),
                p.getCriadoEm());
    }

    public static List<PrescricaoResponse> from(List<Prescricao> prescricoes) {
        return prescricoes.stream().map(PrescricaoResponse::from).toList();
    }
}