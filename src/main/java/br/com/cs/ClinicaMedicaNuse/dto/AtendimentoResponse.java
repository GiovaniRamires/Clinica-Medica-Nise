package br.com.cs.ClinicaMedicaNuse.dto;

import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import br.com.cs.ClinicaMedicaNuse.entity.Prescricao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * O que o paciente vê de um atendimento dele: o resumo da consulta com as
 * prescrições e os exames pedidos nela.
 *
 * Fica de fora, de propósito, tudo que é anotação interna da equipe
 * (observacoesMedicas) e qualquer id de usuário: o paciente enxerga só o que
 * é dele e o que o médico concluiu para ele.
 */
public record AtendimentoResponse(
        Long consultaId,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate data,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime horario,

        MedicoInfo medico,
        String queixaPrincipal,
        String diagnostico,
        String conduta,
        List<PrescricaoItem> prescricoes,
        List<ExameItem> exames
) {

    public record MedicoInfo(String nome, String especialidade, String crm) {
    }

    public record PrescricaoItem(
            String medicamento,
            String dosagem,
            String frequencia,
            String duracao,
            String observacoes
    ) {
        static PrescricaoItem from(Prescricao p) {
            return new PrescricaoItem(p.getMedicamento(), p.getDosagem(), p.getFrequencia(),
                    p.getDuracao(), p.getObservacoes());
        }
    }

    public record ExameItem(
            String nome,
            Exame.SituacaoExame situacao,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate dataSolicitacao,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate dataRealizacao,
            String resultado
    ) {
        static ExameItem from(Exame e) {
            return new ExameItem(e.getNome(), e.getSituacao(), e.getDataSolicitacao(),
                    e.getDataRealizacao(), e.getResultado());
        }
    }

    public static AtendimentoResponse from(Consulta consulta, List<Prescricao> prescricoes, List<Exame> exames) {
        var medico = consulta.getMedico();
        return new AtendimentoResponse(
                consulta.getId(),
                consulta.getData(),
                consulta.getHorario(),
                new MedicoInfo(medico.getNome(), medico.getEspecialidade(), medico.getCrm()),
                consulta.getQueixaPrincipal(),
                consulta.getDiagnostico(),
                consulta.getConduta(),
                prescricoes.stream().map(PrescricaoItem::from).toList(),
                exames.stream().map(ExameItem::from).toList());
    }
}
