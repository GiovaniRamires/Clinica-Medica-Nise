package br.com.cs.ClinicaMedicaNuse.exception;

import br.com.cs.ClinicaMedicaNuse.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Centraliza o tratamento de erros da API para que toda falha saia no mesmo
 * formato ({@link ErroResponse}), em vez de cada controller devolver um corpo
 * diferente (ou um ResponseEntity.build() sem explicação).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Campos que não passaram no Bean Validation dos DTOs de entrada. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        List<ErroResponse.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ErroResponse.deValidacao(HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), campos));
    }

    /** Regras de negócio dos services (404 de id inexistente, 409 de conflito). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(ResponseStatusException ex,
                                                             HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        HttpStatus resolvido = HttpStatus.resolve(status);
        String erro = resolvido != null ? resolvido.getReasonPhrase() : "Erro";
        String mensagem = ex.getReason() != null ? ex.getReason() : erro;

        return ResponseEntity.status(status)
                .body(ErroResponse.de(status, erro, mensagem, request.getRequestURI()));
    }

    /** JSON malformado ou com tipo incompatível (ex: data "31/02/2026"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        log.debug("Corpo da requisição ilegível em {}", request.getRequestURI(), ex);
        return ResponseEntity.badRequest().body(ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Corpo da requisição inválido: verifique o JSON e os formatos de data (yyyy-MM-dd) e horário (HH:mm)",
                request.getRequestURI()));
    }

    /** Qualquer falha não prevista: o detalhe fica no log, não na resposta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ErroResponse.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno no servidor",
                request.getRequestURI()));
    }
}
