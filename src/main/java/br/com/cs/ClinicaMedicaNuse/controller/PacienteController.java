package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.ConsultaResponse;
import br.com.cs.ClinicaMedicaNuse.dto.PacienteRequest;
import br.com.cs.ClinicaMedicaNuse.dto.PacienteResponse;
import br.com.cs.ClinicaMedicaNuse.dto.ProntuarioResponse;
import br.com.cs.ClinicaMedicaNuse.service.ConsultaService;
import br.com.cs.ClinicaMedicaNuse.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<PacienteResponse> criarPaciente(@Valid @RequestBody PacienteRequest request) {
        var paciente = pacienteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteResponse.from(paciente));
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponse>> listarPacientes() {
        return ResponseEntity.ok(PacienteResponse.from(pacienteService.listarPacientes()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PacienteResponse.from(pacienteService.buscarPacientePorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PacienteRequest request) {
        return ResponseEntity.ok(PacienteResponse.from(pacienteService.atualizar(id, request)));
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
