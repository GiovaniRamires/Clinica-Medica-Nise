package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Prescricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescricaoRepository extends JpaRepository<Prescricao, Long> {

    List<Prescricao> findByConsultaIdOrderByCriadoEmDesc(Long consultaId);

    List<Prescricao> findByConsultaPacienteIdOrderByCriadoEmDesc(Long pacienteId);
}
