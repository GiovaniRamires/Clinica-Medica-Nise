package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

}
