package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha do próprio usuário logado. Exige a senha atual para que um
 * token vazado não sirva para tomar a conta.
 */
public record AlterarSenhaRequest(

        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres")
        String novaSenha
) {
}
