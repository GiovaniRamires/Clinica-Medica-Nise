package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.RecepcionistaRequest;
import br.com.cs.ClinicaMedicaNuse.dto.RecepcionistaResponse;
import br.com.cs.ClinicaMedicaNuse.service.RecepcionistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recepcionistas")
public class RecepcionistaController {

    private final RecepcionistaService recepcionistaService;

    public RecepcionistaController(RecepcionistaService recepcionistaService) {
        this.recepcionistaService = recepcionistaService;
    }

    @PostMapping
    public ResponseEntity<RecepcionistaResponse> criar(
            @Valid @RequestBody RecepcionistaRequest request) {
        var recepcionista = recepcionistaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RecepcionistaResponse.from(recepcionista));
    }

    @GetMapping
    public ResponseEntity<List<RecepcionistaResponse>> listar() {
        return ResponseEntity.ok(RecepcionistaResponse.from(recepcionistaService.listarTodas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecepcionistaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(RecepcionistaResponse.from(recepcionistaService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecepcionistaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RecepcionistaRequest request) {
        return ResponseEntity.ok(
                RecepcionistaResponse.from(recepcionistaService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        recepcionistaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
