package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;

/**
 * Versão reduzida do paciente para ser aninhada em agendamentos e consultas.
 * Não expõe alergias nem prontuário: dado clínico só sai pelos endpoints
 * de paciente e de prontuário.
 */
public record PacienteResumoResponse(
        Long id,
        String nome,
        String cpf
) {
    public static PacienteResumoResponse from(Paciente paciente) {
        return new PacienteResumoResponse(paciente.getId(), paciente.getNome(), paciente.getCpf());
    }
}
