package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Recepcionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecepcionistaRepository extends JpaRepository<Recepcionista, Long> {

    boolean existsByMatricula(String matricula);

    /** Usado no update: ignora a própria recepcionista ao checar duplicidade de matrícula. */
    boolean existsByMatriculaAndIdNot(String matricula, Long id);
}
