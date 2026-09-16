package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table (name = "paciente")
@Data
@NoArgsConstructor
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private String nome;

    @Column
    @NotBlank
    private String cpf;

    @Column
    private String convenio;

    @Column
    private String alergias;

    @Column
    private String prontuario;

    public Paciente(String nome, String cpf, String convenio, String alergias, String prontuario) {
        this.nome = nome;
        this.cpf = cpf;
        this.convenio = convenio;
        this.alergias = alergias;
        this.prontuario = prontuario;
    }
}
