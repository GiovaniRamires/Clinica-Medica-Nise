package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.AlterarSituacaoExameRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ExameRequest;
import br.com.cs.ClinicaMedicaNuse.dto.ExameResponse;
import br.com.cs.ClinicaMedicaNuse.dto.ResultadoExameRequest;
import br.com.cs.ClinicaMedicaNuse.service.ExameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exames")
public class ExameController {

    private final ExameService exameService;

    public ExameController(ExameService exameService) {
        this.exameService = exameService;
    }

    @PostMapping
    public ResponseEntity<ExameResponse> solicitar(@Valid @RequestBody ExameRequest request) {
        var exame = exameService.solicitar(request.consultaId(), request.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExameResponse.from(exame));
    }

    @GetMapping
    public ResponseEntity<List<ExameResponse>> listar(
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(required = false) Boolean pendentes) {

        if (pacienteId != null) {
            return ResponseEntity.ok(ExameResponse.from(exameService.listarPorPaciente(pacienteId)));
        }
        if (Boolean.TRUE.equals(pendentes)) {
            return ResponseEntity.ok(ExameResponse.from(exameService.listarPendentes()));
        }
        return ResponseEntity.ok(ExameResponse.from(exameService.listar()));
    }

    @PutMapping("/{id}/situacao")
    public ResponseEntity<ExameResponse> alterarSituacao(@PathVariable Long id,
                                                         @Valid @RequestBody AlterarSituacaoExameRequest request) {
        return ResponseEntity.ok(ExameResponse.from(exameService.alterarSituacao(id, request.situacao())));
    }

    @PutMapping("/{id}/resultado")
    public ResponseEntity<ExameResponse> registrarResultado(@PathVariable Long id,
                                                            @Valid @RequestBody ResultadoExameRequest request) {
        return ResponseEntity.ok(ExameResponse.from(exameService.registrarResultado(id, request.resultado())));
    }
}
