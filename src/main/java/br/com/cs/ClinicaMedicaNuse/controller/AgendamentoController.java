package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.AgendamentoResponse;
import br.com.cs.ClinicaMedicaNuse.dto.AlterarSituacaoRequest;
import br.com.cs.ClinicaMedicaNuse.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criarAgendamento(@Valid @RequestBody AgendamentoRequest request) {
        var agendamento = agendamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.from(agendamento));
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> listarAgendamentos() {
        return ResponseEntity.ok(AgendamentoResponse.from(agendamentoService.listarAgendamentos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(AgendamentoResponse.from(agendamentoService.buscarAgendamentosPorId(id)));
    }

    @PutMapping("/{id}/realizado")
    public ResponseEntity<AgendamentoResponse> marcarComoRealizado(@PathVariable Long id) {
        return ResponseEntity.ok(AgendamentoResponse.from(agendamentoService.marcarComoRealizado(id)));
    }

    @PutMapping("/{id}/situacao")
    public ResponseEntity<AgendamentoResponse> alterarSituacao(@PathVariable Long id,
                                                               @Valid @RequestBody AlterarSituacaoRequest request) {
        return ResponseEntity.ok(AgendamentoResponse.from(
                agendamentoService.alterarSituacao(id, request.situacao())));
    }
}
