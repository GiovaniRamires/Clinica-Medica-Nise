package br.com.cs.ClinicaMedicaNuse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Auto-cadastro público do paciente: cria o Paciente (dados clínicos) e o
 * Usuario (login, perfil PACIENTE) vinculados na mesma operação.
 */
public record CadastroPacienteRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 dígitos numéricos!")
        String cpf,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
        String senha,

        @Size(max = 80, message = "O convênio deve ter no máximo 80 caracteres")
        String convenio
) {
}
