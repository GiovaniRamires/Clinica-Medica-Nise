package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "paciente")
@Data
@NoArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Único no banco: a checagem no service evita o 500, a constraint garante o dado. */
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 80)
    private String convenio;

    @Column(length = 500)
    private String alergias;

    @Column(length = 2000)
    private String prontuario;

    /**
     * Login do próprio paciente, quando ele se cadastrou sozinho
     * (POST /auth/cadastro-paciente). Nulo para pacientes cadastrados pela
     * recepção/administrador sem conta — eles não acessam o sistema.
     */
    @OneToOne(optional = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public Paciente(String nome, String cpf, String convenio, String alergias, String prontuario) {
        this.nome = nome;
        this.cpf = cpf;
        this.convenio = convenio;
        this.alergias = alergias;
        this.prontuario = prontuario;
    }
}
