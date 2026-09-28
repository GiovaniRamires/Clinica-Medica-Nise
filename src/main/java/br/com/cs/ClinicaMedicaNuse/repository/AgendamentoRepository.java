package br.com.cs.ClinicaMedicaNuse.repository;


import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {


    boolean existsMedicoIdAndDataAndHorarioAndSituacaoNot(Long medicoId, LocalDate data, LocalTime horario, SituacaoAgendamento situacao);
}
