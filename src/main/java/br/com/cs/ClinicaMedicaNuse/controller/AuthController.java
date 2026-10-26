package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.*;
import br.com.cs.ClinicaMedicaNuse.security.UsuarioLogado;
import br.com.cs.ClinicaMedicaNuse.service.AgendamentoService;
import br.com.cs.ClinicaMedicaNuse.service.AtendimentoService;
import br.com.cs.ClinicaMedicaNuse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AgendamentoService agendamentoService;
    private final AtendimentoService atendimentoService;

    public AuthController(AuthService authService, AgendamentoService agendamentoService,
                          AtendimentoService atendimentoService) {
        this.authService = authService;
        this.agendamentoService = agendamentoService;
        this.atendimentoService = atendimentoService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Auto-cadastro público: qualquer pessoa cria sua própria conta de
     * paciente. Já devolve o login pronto, como o POST /auth/login.
     */
    @PostMapping("/cadastro-paciente")
    public ResponseEntity<LoginResponse> cadastrarPaciente(@Valid @RequestBody CadastroPacienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.cadastrarPaciente(request));
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

    /**
     * As consultas marcadas do paciente logado. O id do paciente vem do
     * vínculo com o token, nunca de um id na URL que o cliente poderia trocar.
     */
    @GetMapping("/minhas-consultas")
    public ResponseEntity<List<AgendamentoResponse>> minhasConsultas(Principal principal) {
        var paciente = authService.buscarPacienteLogado(UsuarioLogado.id(principal));
        return ResponseEntity.ok(AgendamentoResponse.from(agendamentoService.listarPorPaciente(paciente.getId())));
    }

    /**
     * O retorno dos atendimentos do paciente logado: o que o médico concluiu
     * (diagnóstico e conduta), as prescrições e os exames de cada consulta.
     */
    @GetMapping("/meus-atendimentos")
    public ResponseEntity<List<AtendimentoResponse>> meusAtendimentos(Principal principal) {
        var paciente = authService.buscarPacienteLogado(UsuarioLogado.id(principal));
        return ResponseEntity.ok(atendimentoService.listarDoPaciente(paciente.getId()));
    }

    /** O paciente logado cancela uma consulta dele; o horário volta a ficar disponível. */
    @PutMapping("/minhas-consultas/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelarMinhaConsulta(Principal principal, @PathVariable Long id) {
        var paciente = authService.buscarPacienteLogado(UsuarioLogado.id(principal));
        return ResponseEntity.ok(AgendamentoResponse.from(agendamentoService.cancelarDoPaciente(paciente, id)));
    }

    /** O paciente logado solicita uma consulta; a clínica confirma depois. */
    @PostMapping("/solicitar-agendamento")
    public ResponseEntity<AgendamentoResponse> solicitarAgendamento(Principal principal,
                                                                     @Valid @RequestBody SolicitacaoAgendamentoRequest request) {
        var paciente = authService.buscarPacienteLogado(UsuarioLogado.id(principal));
        var agendamento = agendamentoService.solicitar(paciente, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.from(agendamento));
    }
}
