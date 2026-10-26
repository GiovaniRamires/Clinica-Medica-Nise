package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medico")
@Data
@NoArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** CRM identifica o médico, então é único no banco. */
    @Column(nullable = false, unique = true, length = 10)
    private String crm;

    @Column(nullable = false, length = 80)
    private String especialidade;

    /**
     * Conta de acesso do médico no sistema.
     * Obrigatória: todo médico precisa de login (perfil MEDICO) para atender,
     * e cada usuário só pode estar ligado a um médico.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public Medico(String nome, String crm, String especialidade, Usuario usuario) {
        this.nome = nome;
        this.crm = crm;
        this.especialidade = especialidade;
        this.usuario = usuario;
    }
}
