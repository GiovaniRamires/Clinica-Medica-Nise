package br.com.cs.ClinicaMedicaNuse.service;

import br.com.cs.ClinicaMedicaNuse.dto.AtendimentoResponse;
import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import br.com.cs.ClinicaMedicaNuse.entity.Prescricao;
import br.com.cs.ClinicaMedicaNuse.repository.ConsultaRepository;
import br.com.cs.ClinicaMedicaNuse.repository.ExameRepository;
import br.com.cs.ClinicaMedicaNuse.repository.PrescricaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta, para o paciente, o retorno dos atendimentos dele: o que o médico
 * concluiu na consulta, as prescrições e os exames. Recebe o id do paciente
 * já resolvido a partir do token, nunca de um parâmetro do cliente.
 */
@Service
public class AtendimentoService {

    private final ConsultaRepository consultaRepository;
    private final PrescricaoRepository prescricaoRepository;
    private final ExameRepository exameRepository;

    public AtendimentoService(ConsultaRepository consultaRepository,
                              PrescricaoRepository prescricaoRepository,
                              ExameRepository exameRepository) {
        this.consultaRepository = consultaRepository;
        this.prescricaoRepository = prescricaoRepository;
        this.exameRepository = exameRepository;
    }

    /** Atendimentos do paciente, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<AtendimentoResponse> listarDoPaciente(Long pacienteId) {
        // Três consultas ao banco no total (e não uma por atendimento): busca tudo
        // do paciente e agrupa em memória pela consulta de origem.
        Map<Long, List<Prescricao>> prescricoes = prescricaoRepository
                .findByConsultaPacienteIdOrderByCriadoEmDesc(pacienteId).stream()
                .collect(Collectors.groupingBy(p -> p.getConsulta().getId()));
        Map<Long, List<Exame>> exames = exameRepository
                .findByPacienteIdOrderByDataSolicitacaoDesc(pacienteId).stream()
                .collect(Collectors.groupingBy(e -> e.getConsulta().getId()));

        return consultaRepository.findByPacienteIdOrderByDataDescHorarioDesc(pacienteId).stream()
                .map(c -> AtendimentoResponse.from(c,
                        prescricoes.getOrDefault(c.getId(), List.of()),
                        exames.getOrDefault(c.getId(), List.of())))
                .toList();
    }
}
