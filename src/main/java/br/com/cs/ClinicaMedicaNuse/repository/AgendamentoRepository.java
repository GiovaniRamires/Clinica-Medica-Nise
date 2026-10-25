package br.com.cs.ClinicaMedicaNuse.repository;


import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {


    boolean existsByMedicoIdAndDataAndHorarioAndSituacaoNot(Long medicoId, LocalDate data, LocalTime horario, SituacaoAgendamento situacao);

    /** Mesma regra de agenda cheia, do lado do paciente. */
    boolean existsByPacienteIdAndDataAndHorarioAndSituacaoNot(Long pacienteId, LocalDate data, LocalTime horario, SituacaoAgendamento situacao);

    /** Listagem da agenda em ordem cronológica. */
    List<Agendamento> findAllByOrderByDataAscHorarioAsc();
}
