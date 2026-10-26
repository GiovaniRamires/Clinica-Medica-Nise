package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.PrescricaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.PrescricaoResponse;
import br.com.cs.ClinicaMedicaNuse.service.PrescricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prescricoes")
public class PrescricaoController {

    private final PrescricaoService prescricaoService;

    public PrescricaoController(PrescricaoService prescricaoService) {
        this.prescricaoService = prescricaoService;
    }

    @PostMapping
    public ResponseEntity<PrescricaoResponse> criar(@Valid @RequestBody PrescricaoRequest request) {
        var prescricao = prescricaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PrescricaoResponse.from(prescricao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescricaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PrescricaoResponse.from(prescricaoService.buscarPorId(id)));
    }

    @GetMapping(params = "consultaId")
    public ResponseEntity<List<PrescricaoResponse>> listarPorConsulta(@RequestParam Long consultaId) {
        return ResponseEntity.ok(PrescricaoResponse.from(prescricaoService.listarPorConsulta(consultaId)));
    }

    @GetMapping(params = "pacienteId")
    public ResponseEntity<List<PrescricaoResponse>> listarPorPaciente(@RequestParam Long pacienteId) {
        return ResponseEntity.ok(PrescricaoResponse.from(prescricaoService.listarPorPaciente(pacienteId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescricaoResponse> atualizar(@PathVariable Long id,
                                                        @Valid @RequestBody PrescricaoRequest request) {
        return ResponseEntity.ok(PrescricaoResponse.from(prescricaoService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        prescricaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}