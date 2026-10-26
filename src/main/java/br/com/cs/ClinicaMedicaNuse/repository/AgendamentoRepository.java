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


    boolean existsByPacienteIdAndDataAndHorarioAndSituacaoNot(Long pacienteId, LocalDate data, LocalTime horario, SituacaoAgendamento situacao);


    List<Agendamento> findAllByOrderByDataAscHorarioAsc();


    List<Agendamento> findByPacienteIdOrderByDataAscHorarioAsc(Long pacienteId);

    List<Agendamento> findByMedicoIdAndDataAndSituacaoNot(Long medicoId, LocalDate data, SituacaoAgendamento situacao);
}
