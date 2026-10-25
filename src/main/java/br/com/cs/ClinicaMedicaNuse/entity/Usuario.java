package br.com.cs.ClinicaMedicaNuse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
public class Usuario {

    /**
     * Quem usa o sistema da clínica:
     * - RECEPCIONISTA: cadastra pacientes e cuida da agenda
     * - MEDICO: atende, registra consultas e vê prontuário
     * - ADMINISTRADOR: tudo, incluindo o cadastro de usuários
     */
    public enum Perfil {
        ADMINISTRADOR,
        MEDICO,
        RECEPCIONISTA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt da senha. Nunca é serializado na API nem impresso no toString. */
    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false, length = 72)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }
}
