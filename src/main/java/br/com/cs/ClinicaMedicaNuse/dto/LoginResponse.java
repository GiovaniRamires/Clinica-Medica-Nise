package br.com.cs.ClinicaMedicaNuse.dto;

/**
 * Resposta do login: o token JWT que o front guarda e manda no header
 * Authorization, mais os dados do usuário que a tela mostra. A senha (nem o
 * hash) nunca aparece aqui.
 */
public record LoginResponse(
        String token,
        long expiraEmSegundos,
        UsuarioResponse usuario
) {
}
