package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;

import java.util.List;

public record ProntuarioResponse(Paciente paciente, List<Consulta> consultas) {
}
