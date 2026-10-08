package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
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
    public ResponseEntity<Agendamento> criarAgendamento(@Valid @RequestBody Agendamento agendamento) {
        var novoAgendamento = agendamentoService.criar(
                agendamento.getPaciente().getId(),
                agendamento.getMedico().getId(),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getObservacoes());
        return ResponseEntity.status(HttpStatus.CREATED).body(novoAgendamento);
    }

    @GetMapping
    public ResponseEntity<List<Agendamento>> listarAgendamentos() {
        var agendamentos = agendamentoService.listarAgendamentos();
        return ResponseEntity.status(HttpStatus.OK).body(agendamentos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agendamento> buscarPorId(@PathVariable Long id) {
        var agendamento = agendamentoService.buscarAgendamentosPorId(id);
        return ResponseEntity.status(HttpStatus.OK).body(agendamento);
    }

    @PutMapping("/{id}/realizado")
    public ResponseEntity<Agendamento> marcarComoRealizado(@PathVariable Long id) {
        var agendamento = agendamentoService.buscarAgendamentosPorId(id);
        agendamento = agendamentoService.marcarComoRealizado(agendamento);
        return ResponseEntity.status(HttpStatus.OK).body(agendamento);
    }
}