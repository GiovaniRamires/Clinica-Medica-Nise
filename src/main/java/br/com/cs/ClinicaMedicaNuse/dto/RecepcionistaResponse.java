package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Recepcionista;

import java.util.List;

/** Representação da recepcionista devolvida pela API. */
public record RecepcionistaResponse(
        Long id,
        String nome,
        String matricula,
        String turno,
        Long usuarioId
) {
    public static RecepcionistaResponse from(Recepcionista r) {
        return new RecepcionistaResponse(
                r.getId(),
                r.getNome(),
                r.getMatricula(),
                r.getTurno(),
                r.getUsuario().getId());
    }

    public static List<RecepcionistaResponse> from(List<Recepcionista> lista) {
        return lista.stream().map(RecepcionistaResponse::from).toList();
    }
}
