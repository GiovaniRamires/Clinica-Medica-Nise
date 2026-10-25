package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.MedicoRequest;
import br.com.cs.ClinicaMedicaNuse.dto.MedicoResponse;
import br.com.cs.ClinicaMedicaNuse.service.MedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicos")
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @PostMapping
    public ResponseEntity<MedicoResponse> criarMedico(@Valid @RequestBody MedicoRequest request) {
        var medico = medicoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MedicoResponse.from(medico));
    }

    @GetMapping
    public ResponseEntity<List<MedicoResponse>> listarMedicos() {
        return ResponseEntity.ok(MedicoResponse.from(medicoService.listarMedicos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(MedicoResponse.from(medicoService.buscarMedicoPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicoResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody MedicoRequest request) {
        return ResponseEntity.ok(MedicoResponse.from(medicoService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        medicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
