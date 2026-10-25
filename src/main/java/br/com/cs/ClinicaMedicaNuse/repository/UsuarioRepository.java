package br.com.cs.ClinicaMedicaNuse.repository;

import br.com.cs.ClinicaMedicaNuse.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado no update: ignora o próprio usuário ao checar duplicidade de e-mail. */
    boolean existsByEmailAndIdNot(String email, Long id);

    List<Usuario> findAllByOrderByNomeAsc();

    /** Usado para nao deixar o sistema sem nenhum administrador. */
    long countByPerfil(Usuario.Perfil perfil);
}
