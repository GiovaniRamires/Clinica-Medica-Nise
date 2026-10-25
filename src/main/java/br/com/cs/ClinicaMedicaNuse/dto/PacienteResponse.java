package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;

import java.util.List;

/**
 * Representação completa do paciente, incluindo os dados clínicos.
 *
 * Quem não pode ver dado clínico (recepção) recebe o mesmo formato com
 * alergias e prontuário vazios — ver {@link #semDadoClinico(Paciente)}.
 * Sem isso o 403 do endpoint de prontuário seria inútil, porque a lista de
 * pacientes já entregaria os mesmos campos.
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

    /** Mesmo paciente, sem os campos clínicos. */
    public static PacienteResponse semDadoClinico(Paciente paciente) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getConvenio(),
                null,
                null);
    }

    public static PacienteResponse de(Paciente paciente, boolean comDadoClinico) {
        return comDadoClinico ? from(paciente) : semDadoClinico(paciente);
    }

    public static List<PacienteResponse> from(List<Paciente> pacientes) {
        return pacientes.stream().map(PacienteResponse::from).toList();
    }

    public static List<PacienteResponse> de(List<Paciente> pacientes, boolean comDadoClinico) {
        return pacientes.stream().map(paciente -> de(paciente, comDadoClinico)).toList();
    }
}
