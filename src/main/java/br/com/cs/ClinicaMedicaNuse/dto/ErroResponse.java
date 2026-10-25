package br.com.cs.ClinicaMedicaNuse.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo padrão de erro da API. O front-end lê sempre "mensagem" e, quando
 * o erro é de validação, detalha campo por campo em "campos".
 */
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        LocalDateTime momento,
        List<CampoInvalido> campos
) {
    /** Um campo que não passou na validação, com o motivo. */
    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(status, erro, mensagem, caminho, LocalDateTime.now(), List.of());
    }

    public static ErroResponse deValidacao(int status, String caminho, List<CampoInvalido> campos) {
        return new ErroResponse(status, "Bad Request", "Há campos inválidos na requisição",
                caminho, LocalDateTime.now(), campos);
    }
}
