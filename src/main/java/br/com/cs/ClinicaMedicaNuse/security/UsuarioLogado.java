package br.com.cs.ClinicaMedicaNuse.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Obtém o id do usuário autenticado (subject do JWT) a partir do Principal.
 * O id nunca vem do corpo da requisição: quem diz quem está logado é o token.
 */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static Long id(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }

    /**
     * Se o autenticado pode ver/gravar dado clínico (alergias e prontuário).
     * A recepção cuida do cadastro e da agenda, mas não do dado clínico.
     */
    public static boolean podeVerDadoClinico(Authentication autenticacao) {
        if (autenticacao == null) {
            return false;
        }
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(papel -> papel.equals("ROLE_MEDICO") || papel.equals("ROLE_ADMINISTRADOR"));
    }
}
