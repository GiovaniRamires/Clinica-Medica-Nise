package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Medico;

import java.util.List;

/** Representação do médico devolvida pela API. */
public record MedicoResponse(
        Long id,
        String nome,
        String crm,
        String especialidade
) {
    public static MedicoResponse from(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNome(),
                medico.getCrm(),
                medico.getEspecialidade());
    }

    public static List<MedicoResponse> from(List<Medico> medicos) {
        return medicos.stream().map(MedicoResponse::from).toList();
    }
}
