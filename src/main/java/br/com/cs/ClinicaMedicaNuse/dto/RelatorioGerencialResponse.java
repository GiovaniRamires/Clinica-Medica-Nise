package br.com.cs.ClinicaMedicaNuse.dto;

import java.util.List;

/**
 * Consolida os indicadores gerenciais da clínica (PA-19). Não inclui dados
 * financeiros/faturamento, que estão fora do escopo deste projeto acadêmico.
 * <p>
 * Todos os números são agregações feitas sobre os dados já existentes:
 * agendamentos, consultas realizadas, exames e cadastros.
 */
public record RelatorioGerencialResponse(
        long totalPacientes,
        long totalMedicos,
        long totalAgendamentos,
        long totalConsultasRealizadas,
        long totalExames,
        long totalFaltas,
        double taxaComparecimento,
        double taxaFaltas,
        List<ContagemPorRotulo> agendamentosPorSituacao,
        List<ContagemPorRotulo> examesPorSituacao,
        List<ContagemPorRotulo> consultasPorMedico,
        List<ContagemPorRotulo> examesMaisSolicitados
) {
    /** Par rótulo/quantidade usado nas listas do relatório. */
    public record ContagemPorRotulo(String rotulo, long quantidade) {
    }
}
