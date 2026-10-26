package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recepcionista")
@Data
@NoArgsConstructor
public class Recepcionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Matrícula identifica a recepcionista, então é única no banco. */
    @Column(nullable = false, unique = true, length = 20)
    private String matricula;

    @Column(nullable = false, length = 20)
    private String turno;

    /**
     * Conta de acesso da recepcionista no sistema.
     * Obrigatória: toda recepcionista precisa de login para operar.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public Recepcionista(String nome, String matricula, String turno, Usuario usuario) {
        this.nome = nome;
        this.matricula = matricula;
        this.turno = turno;
        this.usuario = usuario;
    }
}
