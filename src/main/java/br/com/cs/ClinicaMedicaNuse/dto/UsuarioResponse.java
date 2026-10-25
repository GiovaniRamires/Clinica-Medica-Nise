package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;

import java.util.List;

/** Usuário do sistema como a API devolve: sem senha e sem hash. */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Usuario.Perfil perfil
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil());
    }

    public static List<UsuarioResponse> from(List<Usuario> usuarios) {
        return usuarios.stream().map(UsuarioResponse::from).toList();
    }
}
