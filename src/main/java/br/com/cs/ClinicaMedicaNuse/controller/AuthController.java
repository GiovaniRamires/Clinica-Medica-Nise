package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.AlterarSenhaRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginRequest;
import br.com.cs.ClinicaMedicaNuse.dto.LoginResponse;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioResponse;
import br.com.cs.ClinicaMedicaNuse.security.UsuarioLogado;
import br.com.cs.ClinicaMedicaNuse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * O JWT é stateless: não existe sessão no servidor para encerrar. O front
     * descarta o token ao sair; este endpoint existe para o contrato do cliente.
     * O token continua válido até expirar.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    /** Quem está logado, segundo o token. O front chama ao recarregar a página. */
    @GetMapping("/eu")
    public ResponseEntity<UsuarioResponse> eu(Principal principal) {
        var usuario = authService.buscarLogado(UsuarioLogado.id(principal));
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    /** Troca da própria senha. O id vem do token, nunca do corpo da requisição. */
    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(Principal principal,
                                             @Valid @RequestBody AlterarSenhaRequest request) {
        authService.alterarSenha(UsuarioLogado.id(principal), request);
        return ResponseEntity.noContent().build();
    }
}
