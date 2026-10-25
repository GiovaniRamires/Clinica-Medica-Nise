package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;

import java.util.List;

/**
 * Representação completa do paciente, incluindo os dados clínicos.
 * Usada apenas nos endpoints do próprio paciente e do prontuário.
 */
public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        String convenio,
        String alergias,
        String prontuario
) {
    public static PacienteResponse from(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getConvenio(),
                paciente.getAlergias(),
                paciente.getProntuario());
    }

    public static List<PacienteResponse> from(List<Paciente> pacientes) {
        return pacientes.stream().map(PacienteResponse::from).toList();
    }
}
