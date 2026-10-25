package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.UsuarioAtualizacaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioRequest;
import br.com.cs.ClinicaMedicaNuse.dto.UsuarioResponse;
import br.com.cs.ClinicaMedicaNuse.security.UsuarioLogado;
import br.com.cs.ClinicaMedicaNuse.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Cadastro dos usuários do sistema. Todo este controller é restrito ao perfil
 * ADMINISTRADOR pelas regras do SecurityConfig.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criarUsuario(@Valid @RequestBody UsuarioRequest request) {
        var usuario = usuarioService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.listarUsuarios()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.buscarUsuarioPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Principal principal) {
        usuarioService.deletar(id, UsuarioLogado.id(principal));
        return ResponseEntity.noContent().build();
    }
}
