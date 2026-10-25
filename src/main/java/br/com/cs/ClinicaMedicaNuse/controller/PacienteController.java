package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.ConsultaResponse;
import br.com.cs.ClinicaMedicaNuse.dto.PacienteRequest;
import br.com.cs.ClinicaMedicaNuse.dto.PacienteResponse;
import br.com.cs.ClinicaMedicaNuse.dto.ProntuarioResponse;
import br.com.cs.ClinicaMedicaNuse.security.UsuarioLogado;
import br.com.cs.ClinicaMedicaNuse.service.ConsultaService;
import br.com.cs.ClinicaMedicaNuse.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cadastro de pacientes.
 *
 * Alergias e prontuário são dado clínico: só saem (e só entram) para MEDICO e
 * ADMINISTRADOR. A recepção enxerga o mesmo paciente com esses campos vazios,
 * senão o 403 de /pacientes/{id}/prontuario não protegeria nada — a listagem
 * entregaria os mesmos campos.
 */
@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final ConsultaService consultaService;

    public PacienteController(PacienteService pacienteService, ConsultaService consultaService) {
        this.pacienteService = pacienteService;
        this.consultaService = consultaService;
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> criarPaciente(@Valid @RequestBody PacienteRequest request,
                                                          Authentication autenticacao) {
        boolean clinico = UsuarioLogado.podeVerDadoClinico(autenticacao);
        var paciente = pacienteService.criar(request, clinico);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteResponse.de(paciente, clinico));
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponse>> listarPacientes(Authentication autenticacao) {
        return ResponseEntity.ok(PacienteResponse.de(
                pacienteService.listarPacientes(),
                UsuarioLogado.podeVerDadoClinico(autenticacao)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id, Authentication autenticacao) {
        return ResponseEntity.ok(PacienteResponse.de(
                pacienteService.buscarPacientePorId(id),
                UsuarioLogado.podeVerDadoClinico(autenticacao)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PacienteRequest request,
                                                      Authentication autenticacao) {
        boolean clinico = UsuarioLogado.podeVerDadoClinico(autenticacao);
        var paciente = pacienteService.atualizar(id, request, clinico);
        return ResponseEntity.ok(PacienteResponse.de(paciente, clinico));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pacienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/consultas")
    public ResponseEntity<List<ConsultaResponse>> historico(@PathVariable Long id) {
        pacienteService.buscarPacientePorId(id);
        return ResponseEntity.ok(ConsultaResponse.from(consultaService.listarPorPaciente(id)));
    }

    @GetMapping("/{id}/prontuario")
    public ResponseEntity<ProntuarioResponse> prontuario(@PathVariable Long id) {
        var paciente = pacienteService.buscarPacientePorId(id);
        return ResponseEntity.ok(ProntuarioResponse.from(paciente, consultaService.listarPorPaciente(id)));
    }
}
