package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByCpf(String cpf);

    /** Usado no update: ignora o próprio paciente ao checar duplicidade de CPF. */
    boolean existsByCpfAndIdNot(String cpf, Long id);

    /** O paciente logado só pode ver/editar o próprio registro clínico. */
    Optional<Paciente> findByUsuarioId(Long usuarioId);
}
