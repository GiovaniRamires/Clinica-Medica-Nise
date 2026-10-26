package br.com.cs.ClinicaMedicaNuse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Table(name = "prescricao")
@Getter
@Setter
@NoArgsConstructor
public class Prescricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @Column(nullable = false, length = 200)
    private String medicamento;

    @Column(nullable = false, length = 100)
    private String dosagem;

    @Column(nullable = false, length = 100)
    private String frequencia;

    @Column(length = 100)
    private String duracao;

    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public Prescricao(Consulta consulta, String medicamento, String dosagem,
                      String frequencia, String duracao, String observacoes) {
        this.consulta = consulta;
        this.medicamento = medicamento;
        this.dosagem = dosagem;
        this.frequencia = frequencia;
        this.duracao = duracao;
        this.observacoes = observacoes;
        this.criadoEm = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Prescricao other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}