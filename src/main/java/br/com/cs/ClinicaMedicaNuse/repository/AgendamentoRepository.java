package br.com.cs.ClinicaMedicaNuse.repository;


import br.com.cs.ClinicaMedicaNuse.entity.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Agendamento.SituacaoAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /** Projeção rótulo/quantidade usada pelos relatórios gerenciais (PA-19). */
    interface ContagemRotulo {
        String getRotulo();
        long getQuantidade();
    }

    long countBySituacao(SituacaoAgendamento situacao);

    @Query("""
            select a.situacao as rotulo, count(a) as quantidade
            from Agendamento a
            group by a.situacao
            order by count(a) desc""")
    List<ContagemRotulo> contarPorSituacao();


    boolean existsByMedicoIdAndDataAndHorarioAndSituacaoNotIn(Long medicoId, LocalDate data, LocalTime horario, Collection<SituacaoAgendamento> situacoes);


    boolean existsByPacienteIdAndDataAndHorarioAndSituacaoNotIn(Long pacienteId, LocalDate data, LocalTime horario, Collection<SituacaoAgendamento> situacoes);


    List<Agendamento> findAllByOrderByDataAscHorarioAsc();


    List<Agendamento> findByPacienteIdOrderByDataAscHorarioAsc(Long pacienteId);

    List<Agendamento> findByMedicoIdAndDataAndSituacaoNotIn(Long medicoId, LocalDate data, Collection<SituacaoAgendamento> situacoes);
}
