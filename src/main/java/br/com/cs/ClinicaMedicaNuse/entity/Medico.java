package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table (name = "medico")
@Data
@NoArgsConstructor
public class Medico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    private String nome;

    @Column
    @NotBlank
    private String crm;

    @Column
    @NotBlank
    private String especialidade;

    public Medico(String nome, String crm, String especialidade) {
        this.nome = nome;
        this.crm = crm;
        this.especialidade = especialidade;
    }
}
