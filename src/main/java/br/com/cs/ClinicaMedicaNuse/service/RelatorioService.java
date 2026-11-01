package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.RelatorioGerencialResponse;
import br.com.cs.ClinicaMedicaNuse.dto.RelatorioGerencialResponse.ContagemPorRotulo;
import br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository.ContagemRotulo;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ExameRepository;
import br.com.cs.ClinicaMedicaNuse.repository.MedicoRepository;
import br.com.cs.ClinicaMedicaNuse.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultas gerenciais da clínica (PA-19): indicadores operacionais calculados
 * por agregação sobre os dados existentes. Sem faturamento/financeiro, que está
 * fora do escopo do projeto.
 */
@Service
public class RelatorioService {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ConsultaRepository consultaRepository;
    private final ExameRepository exameRepository;

    public RelatorioService(PacienteRepository pacienteRepository,
                            MedicoRepository medicoRepository,
                            AgendamentoRepository agendamentoRepository,
                            ConsultaRepository consultaRepository,
                            ExameRepository exameRepository) {
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.consultaRepository = consultaRepository;
        this.exameRepository = exameRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioGerencialResponse gerar() {
        long totalPacientes = pacienteRepository.count();
        long totalMedicos = medicoRepository.count();
        long totalAgendamentos = agendamentoRepository.count();
        long totalConsultas = consultaRepository.count();
        long totalExames = exameRepository.count();

        long faltas = agendamentoRepository.countBySituacao(SituacaoAgendamento.AUSENTE);
        long realizados = agendamentoRepository.countBySituacao(SituacaoAgendamento.REALIZADO);

        // Base de cálculo das taxas: atendimentos que de fato tiveram um desfecho
        // de comparecimento (realizados + faltas). Agendamentos ainda abertos ou
        // cancelados não entram, para a taxa refletir só o que já aconteceu.
        long baseComparecimento = realizados + faltas;
        double taxaComparecimento = baseComparecimento == 0 ? 0.0
                : arredondar(realizados * 100.0 / baseComparecimento);
        double taxaFaltas = baseComparecimento == 0 ? 0.0
                : arredondar(faltas * 100.0 / baseComparecimento);

        return new RelatorioGerencialResponse(
                totalPacientes,
                totalMedicos,
                totalAgendamentos,
                totalConsultas,
                totalExames,
                faltas,
                taxaComparecimento,
                taxaFaltas,
                mapear(agendamentoRepository.contarPorSituacao()),
                mapear(exameRepository.contarPorSituacao()),
                mapear(consultaRepository.contarPorMedico()),
                mapear(exameRepository.contarPorNome()));
    }

    private static List<ContagemPorRotulo> mapear(List<ContagemRotulo> linhas) {
        return linhas.stream()
                .map(l -> new ContagemPorRotulo(l.getRotulo(), l.getQuantidade()))
                .toList();
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
