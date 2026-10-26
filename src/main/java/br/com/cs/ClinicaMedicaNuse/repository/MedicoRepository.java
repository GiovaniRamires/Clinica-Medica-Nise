package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    boolean existsByCrm(String crm);

    /** Usado no update: ignora o próprio médico ao checar duplicidade de CRM. */
    boolean existsByCrmAndIdNot(String crm, Long id);

    /** Um usuário só pode estar ligado a um médico. */
    boolean existsByUsuarioId(Long usuarioId);

    /** Usado no update: ignora o próprio médico ao checar se o usuário já está em uso. */
    boolean existsByUsuarioIdAndIdNot(Long usuarioId, Long id);
}
