package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import br.com.cs.ClinicaMedicaNuse.service.MedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping ("/medicos")
public class MedicoController {
    private final MedicoService medicoService;
    public MedicoController (MedicoService medicoService){
        this.medicoService = medicoService;
    }
    @PostMapping
    public ResponseEntity<Medico> criarMedico (@Valid @RequestBody Medico medico){
        var novoMedico = medicoService.criar(medico.getNome(), medico.getCrm(), medico.getEspecialidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(novoMedico);
    }

    @GetMapping
    public ResponseEntity<List<Medico>> listarMedicos (){
        var medicos = medicoService.listarMedicos();
        return ResponseEntity.status(HttpStatus.OK).body(medicos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> buscarPorId (@PathVariable Long id){
        var medico = medicoService.buscarMedicoPorId(id);
        return ResponseEntity.status(HttpStatus.OK).body(medico);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<Medico> atualizar (@PathVariable Long id, @Valid @RequestBody Medico medico){
        medico = medicoService.atualizar(id, medico.getNome(), medico.getCrm(), medico.getEspecialidade());
        return ResponseEntity.status(HttpStatus.OK).body(medico);
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> deletar (@PathVariable Long id){
        medicoService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
