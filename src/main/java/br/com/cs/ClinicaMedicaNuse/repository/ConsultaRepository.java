package br.com.cs.ClinicaMedicaNuse.repository;
 
import br.com.cs.ClinicaMedicaNuse.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByPacienteIdOrderByDataDescHorarioDesc(Long pacienteId);
}