package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.ProntuarioResponse;
import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
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
    public PacienteController (PacienteService pacienteService, ConsultaService consultaService){
        this.pacienteService = pacienteService;
        this.consultaService = consultaService;
    }
    @PostMapping
    public ResponseEntity<Paciente> criarPaciente (@Valid @RequestBody Paciente paciente){
        var pacienteNovo = pacienteService.criar(paciente.getNome(), paciente.getCpf(), paciente.getConvenio(), paciente.getAlergias(), paciente.getProntuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteNovo);
    }
    @GetMapping
    public ResponseEntity<List<Paciente>> listarPacientes(){
        var pacientes = pacienteService.listarPacientes();
        return ResponseEntity.status(HttpStatus.OK).body(pacientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> buscarPorId(@PathVariable Long id){
        var paciente = pacienteService.buscarPacientePorId(id);
        return ResponseEntity.status(HttpStatus.OK).body(paciente);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<Paciente> atualizar(@PathVariable Long id, @Valid @RequestBody Paciente paciente){
        paciente = pacienteService.atualizar(id, paciente.getNome(), paciente.getCpf(), paciente.getConvenio(), paciente.getAlergias(), paciente.getProntuario());
        return ResponseEntity.status(HttpStatus.OK).body(paciente);
    }
    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> deletar (@PathVariable Long id){
        pacienteService.deletar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{id}/consultas")
    public ResponseEntity<List<Consulta>> historico(@PathVariable Long id){
        pacienteService.buscarPacientePorId(id);
        return ResponseEntity.ok(consultaService.listarPorPaciente(id));
    }

    @GetMapping("/{id}/prontuario")
    public ResponseEntity<ProntuarioResponse> prontuario(@PathVariable Long id){
        return ResponseEntity.ok(new ProntuarioResponse(
                pacienteService.buscarPacientePorId(id), consultaService.listarPorPaciente(id)));
    }
}
