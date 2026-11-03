package br.com.cs.ClinicaMedicaNuse.repository;
 
import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository.ContagemRotulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByPacienteIdOrderByDataDescHorarioDesc(Long pacienteId);

    /** Listagem geral já ordenada da consulta mais recente para a mais antiga. */
    List<Consulta> findAllByOrderByDataDescHorarioDesc();

    /** Consultas efetivamente realizadas agrupadas por médico. */
    @Query("""
            select concat(m.nome, ' (', m.especialidade, ')') as rotulo, count(c) as quantidade
            from Consulta c join c.medico m
            group by m.id, m.nome, m.especialidade
            order by count(c) desc""")
    List<ContagemRotulo> contarPorMedico();
}
