package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Exame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import static br.com.cs.ClinicaMedicaNuse.entity.Exame.SituacaoExame;

@Repository
public interface ExameRepository extends JpaRepository<Exame, Long> {

    List<Exame> findByPacienteIdOrderByDataSolicitacaoDesc(Long pacienteId);

    List<Exame> findBySituacaoNotOrderByDataSolicitacaoDesc(SituacaoExame situacao);

    List<Exame> findAllByOrderByDataSolicitacaoDesc();
}
