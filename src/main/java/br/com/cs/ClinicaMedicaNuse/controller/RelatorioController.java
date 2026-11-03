package br.com.cs.ClinicaMedicaNuse.controller;

import br.com.cs.ClinicaMedicaNuse.dto.RelatorioGerencialResponse;
import br.com.cs.ClinicaMedicaNuse.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios e consultas gerenciais (PA-19). O acesso é restrito à equipe da
 * clínica pelo SecurityConfig; o paciente não enxerga indicadores gerais.
 */
@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/gerencial")
    public ResponseEntity<RelatorioGerencialResponse> gerencial() {
        return ResponseEntity.ok(relatorioService.gerar());
    }
}
