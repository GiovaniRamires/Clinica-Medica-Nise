package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.ConsultaAtualizacaoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ConsultaRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ConsultaResponse;
import br.com.cs.ClinicaMedicaNuse.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> criarConsulta(@Valid @RequestBody ConsultaRequest request) {
        var consulta = consultaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConsultaResponse.from(consulta));
    }

    @GetMapping
    public ResponseEntity<List<ConsultaResponse>> listarConsultas() {
        return ResponseEntity.ok(ConsultaResponse.from(consultaService.listarConsultas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ConsultaResponse.from(consultaService.buscarConsultaPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultaResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ConsultaAtualizacaoRequest request) {
        return ResponseEntity.ok(ConsultaResponse.from(consultaService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        consultaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
