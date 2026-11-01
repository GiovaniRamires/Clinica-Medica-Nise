package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import br.com.cs.ClinicaMedicaNuse.repository.AgendamentoRepository.ContagemRotulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Exame.SituacaoExame;

@Repository
public interface ExameRepository extends JpaRepository<Exame, Long> {

    List<Exame> findByPacienteIdOrderByDataSolicitacaoDesc(Long pacienteId);

    List<Exame> findBySituacaoNotOrderByDataSolicitacaoDesc(SituacaoExame situacao);

    List<Exame> findAllByOrderByDataSolicitacaoDesc();

    /** Exames agrupados por situação. */
    @Query("""
            select e.situacao as rotulo, count(e) as quantidade
            from Exame e
            group by e.situacao
            order by count(e) desc""")
    List<ContagemRotulo> contarPorSituacao();

    /** Exames mais solicitados, pelo nome. */
    @Query("""
            select e.nome as rotulo, count(e) as quantidade
            from Exame e
            group by e.nome
            order by count(e) desc""")
    List<ContagemRotulo> contarPorNome();
}
